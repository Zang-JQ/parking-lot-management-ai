/* ==========================================================================
   Toast —— 右下角轻提示，替代满屏的 alert()
   用法：toast('保存成功')；也可以 toast('保存失败', 'err')
   兼容：会接管 window.alert，历史页面里的 alert(...) 自动升级成 toast，业务代码不用改。
   ========================================================================== */
(function () {
    var box = null;

    function ensureBox() {
        if (box && document.body.contains(box)) {
            return box;
        }
        box = document.createElement('div');
        box.id = 'toastBox';
        document.body.appendChild(box);
        return box;
    }

    /** 没有显式指定类型时，按文案猜一个：失败/错误 = 红，成功 = 绿，其余灰 */
    function guessKind(msg) {
        var s = String(msg);
        if (/(失败|错误|不能|请先|请输入|请选择|不一致|不存在|无效|超时|异常|拒绝)/.test(s)) {
            return 'err';
        }
        if (/(成功|已保存|已更新|已成为|欢迎|一路顺风|完成|已清空)/.test(s)) {
            return 'ok';
        }
        return 'info';
    }

    function show(msg, kind) {
        var el = document.createElement('div');
        el.className = 'toast ' + (kind || guessKind(msg));
        el.textContent = String(msg);
        ensureBox().appendChild(el);
        // 强制重排，让进入动画生效
        void el.offsetWidth;
        el.classList.add('in');
        setTimeout(function () {
            el.classList.remove('in');
            setTimeout(function () {
                if (el.parentNode) {
                    el.parentNode.removeChild(el);
                }
            }, 220);
        }, 2600);
    }

    window.toast = show;

    // 接管全局 alert：老页面里的 alert(...) 现在都是右下角提示
    window.alert = function (msg) {
        show(msg);
    };
})();
