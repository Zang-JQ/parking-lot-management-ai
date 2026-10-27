/* ==========================================================================
   管理端公共外壳：侧边菜单高亮 + 点击跳转
   四个管理页共用，避免每页复制一份导航脚本。
   用法：bindNav('members')
   ========================================================================== */
function bindNav(active) {
    var pages = {
        dashboard: {li: 'li1', file: 'dashboard'},
        spots: {li: 'li2', file: 'spots'},
        rules: {li: 'li3', file: 'rules'},
        records: {li: 'li4', file: 'records'},
        members: {li: 'li5', file: 'members'},
        reports: {li: 'li6', file: 'reports'},
        settings: {li: 'li7', file: 'settings'}
    };
    var username = new URLSearchParams(window.location.search).get('username') || '';

    Object.keys(pages).forEach(function (key) {
        var item = pages[key];
        $('[name=' + item.li + ']').click(function () {
            window.location.href = item.file + '.html?username=' + encodeURIComponent(username);
        });
    });

    if (pages[active]) {
        $('[name=' + pages[active].li + '] a').addClass('active');
    }
}
