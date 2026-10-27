package com.jr.util;

import lombok.extern.slf4j.Slf4j;
import org.csource.common.NameValuePair;
import org.csource.fastdfs.ClientGlobal;
import org.csource.fastdfs.StorageClient;
import org.csource.fastdfs.StorageServer;
import org.csource.fastdfs.TrackerClient;
import org.csource.fastdfs.TrackerServer;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * FastDFS 工具类。
 *
 * 改造要点（原来是 static 初始化块，第一次失败就整个类永久报废）：
 *   1. 懒加载：第一次真正上传/下载时才建连接，不拖慢应用启动；
 *   2. 失败可重连：任何一次调用异常都会 reset()，下次调用重新初始化；
 *   3. tracker 上没有可用 storage 时给出明确原因（而不是 NPE 或干等超时）。
 *
 * 对外用法不变：FastDFSUtils.uploadFile(...) / deleteFile(...)，失败返回 null / -1。
 */
@Slf4j
public final class FastDFSUtils {

    private static final String CONFIG_FILE = "fast.properties";

    /** 连接句柄：null 表示尚未初始化，或上次失败已被重置 */
    private static volatile StorageClient storageClient;
    private static volatile TrackerServer trackerServer;

    private FastDFSUtils() {
    }

    /**
     * 取 StorageClient，没有就初始化。双检锁保证只初始化一次。
     */
    private static StorageClient client() throws Exception {
        StorageClient client = storageClient;
        if (client != null) {
            return client;
        }
        synchronized (FastDFSUtils.class) {
            if (storageClient == null) {
                Properties properties = new Properties();
                try (InputStream in = FastDFSUtils.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
                    if (in == null) {
                        throw new IllegalStateException("classpath 下找不到 " + CONFIG_FILE);
                    }
                    properties.load(in);
                }
                ClientGlobal.initByProperties(properties);

                TrackerClient trackerClient = new TrackerClient();
                TrackerServer tracker = trackerClient.getConnection();
                if (tracker == null) {
                    throw new IllegalStateException("连不上 tracker，检查 tracker 是否启动、22122 是否放行");
                }
                StorageServer storage = trackerClient.getStoreStorage(tracker);
                if (storage == null) {
                    throw new IllegalStateException("tracker 上没有可用的 storage（检查 storage 容器是否注册成功、23000 是否放行）");
                }
                trackerServer = tracker;
                storageClient = new StorageClient(tracker, storage);
                log.info("[FastDFS] 初始化成功：storage={}", storage);
            }
            return storageClient;
        }
    }

    /** 丢弃当前连接，下次调用重新初始化。调用失败时自动触发，也可以手动调。 */
    public static void reset() {
        synchronized (FastDFSUtils.class) {
            storageClient = null;
            trackerServer = null;
        }
    }

    /**
     * 上传文件（流）。
     *
     * @return [group, remoteFileName]，失败返回 null
     */
    public static String[] uploadFile(InputStream inputStream, String fileName) {
        if (inputStream == null) {
            log.warn("[FastDFS] 上传失败：输入流为空");
            return null;
        }
        try {
            // 先整体读成 byte[]，顺便拿到真实长度（原来用 available() 拿长度是不准的）
            byte[] bytes = inputStream.readAllBytes();
            NameValuePair[] metaList = new NameValuePair[]{
                    new NameValuePair("file name", fileName),
                    new NameValuePair("file length", String.valueOf(bytes.length))
            };
            return client().upload_file(bytes, getFileExt(fileName), metaList);
        } catch (Exception e) {
            log.error("[FastDFS] 上传失败（已重置连接，下次重连）：{}", e.getMessage());
            reset();
            return null;
        }
    }

    /**
     * 上传文件（File）。
     *
     * @return [group, remoteFileName]，失败返回 null
     */
    public static String[] uploadFile(File file, String fileName) {
        if (file == null || !file.isFile()) {
            log.warn("[FastDFS] 上传失败：文件不存在 {}", file);
            return null;
        }
        try (InputStream in = new FileInputStream(file)) {
            return uploadFile(in, fileName);
        } catch (IOException e) {
            log.error("[FastDFS] 读取本地文件失败：{}", e.getMessage());
            return null;
        }
    }

    /**
     * 删除文件。
     *
     * @return 0 成功；-1 失败
     */
    public static int deleteFile(String groupName, String remoteFileName) {
        try {
            return client().delete_file(groupName == null ? "group1" : groupName, remoteFileName);
        } catch (Exception e) {
            log.error("[FastDFS] 删除失败（已重置连接）：{}", e.getMessage());
            reset();
            return -1;
        }
    }

    /**
     * 先上传新文件，成功后再删旧文件。
     *
     * @return 新文件的 [group, remoteFileName]；任一步失败返回 null（此时旧文件保持不动）
     */
    public static String[] modifyFile(String oldGroupName, String oldFileName, File file, String fileName) {
        String[] fileIds = uploadFile(file, fileName);
        if (fileIds == null) {
            return null;
        }
        int delResult = deleteFile(oldGroupName, oldFileName);
        if (delResult != 0) {
            log.warn("[FastDFS] 新文件已上传，但旧文件删除失败：{}/{}", oldGroupName, oldFileName);
            return null;
        }
        return fileIds;
    }

    /**
     * 下载文件。
     *
     * @return 文件流；失败返回 null
     */
    public static InputStream downloadFile(String groupName, String remoteFileName) {
        try {
            byte[] bytes = client().download_file(groupName, remoteFileName);
            return bytes == null ? null : new ByteArrayInputStream(bytes);
        } catch (Exception e) {
            log.error("[FastDFS] 下载失败（已重置连接）：{}", e.getMessage());
            reset();
            return null;
        }
    }

    /**
     * 取文件元信息。
     */
    public static NameValuePair[] getMetaDate(String groupName, String remoteFileName) {
        try {
            return client().get_metadata(groupName, remoteFileName);
        } catch (Exception e) {
            log.error("[FastDFS] 读取元信息失败（已重置连接）：{}", e.getMessage());
            reset();
            return null;
        }
    }

    /**
     * 取底层客户端（一般用不到）。连不上时返回 null，不抛异常。
     */
    public static StorageClient getStorageClient() {
        try {
            return client();
        } catch (Exception e) {
            log.error("[FastDFS] 获取客户端失败：{}", e.getMessage());
            reset();
            return null;
        }
    }

    /**
     * 获取文件后缀名（不带点）。
     *
     * @return 如 "jpg"；没有后缀返回 ""
     */
    private static String getFileExt(String fileName) {
        if (fileName == null || fileName.isEmpty() || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }
}
