/* ==========================================================================
   MiniChart —— 零依赖 SVG 迷你图表
   为什么不用 ECharts：答辩现场不一定有网，CDN 挂了就是白屏；
   这里手写 SVG，一张图约 60 行，体积 0，离线可用。
   ========================================================================== */
(function (global) {

    function esc(s) {
        return String(s).replace(/[&<>"]/g, function (c) {
            return {'&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;'}[c];
        });
    }

    /** 金额/数量的小数处理：整数不带小数点 */
    function fmt(v) {
        var n = Number(v) || 0;
        return n % 1 === 0 ? String(n) : n.toFixed(1);
    }

    /**
     * 把最大值向上取整成"好看"的刻度上限。
     * 关键：保证 /4 之后每个刻度也是整数，避免出现 7.5、22.5 这种金额刻度。
     */
    function niceMax(v) {
        if (!(v > 0)) {
            return 4;
        }
        var raw = v / 4;                                  // 每格需要的量
        var p = Math.pow(10, Math.floor(Math.log10(raw)));
        var n = raw / p;
        var m = n <= 1 ? 1 : n <= 2 ? 2 : n <= 2.5 ? 2.5 : n <= 4 ? 4 : n <= 5 ? 5 : n <= 8 ? 8 : 10;
        return m * p * 4;
    }

    /**
     * 折线图（带渐变面积）
     * @param el   容器
     * @param data [{label:'10-21', value: 12}, ...]
     * @param opt  {color}
     */
    function line(el, data, opt) {
        opt = opt || {};
        var color = opt.color || '#0ea5e9';
        data = data || [];
        if (!el || data.length === 0) {
            if (el) {
                el.innerHTML = '<div class="chart-empty">暂无数据</div>';
            }
            return;
        }

        var W = 620, H = 220, L = 46, R = 16, T = 18, B = 30;
        var iw = W - L - R, ih = H - T - B;
        var n = data.length;
        var max = niceMax(Math.max.apply(null, data.map(function (d) {
            return d.value;
        })));
        var step = n > 1 ? iw / (n - 1) : 0;

        function x(i) { return n > 1 ? L + i * step : L + iw / 2; }
        function y(v) { return T + ih - (v / max) * ih; }

        var out = '';

        // 网格 + Y 轴刻度
        for (var g = 0; g <= 4; g++) {
            var gv = max * g / 4;
            var gy = y(gv);
            out += '<line x1="' + L + '" y1="' + gy + '" x2="' + (W - R) + '" y2="' + gy
                + '" stroke="#eef2f7" stroke-width="1"/>';
            out += '<text x="' + (L - 8) + '" y="' + (gy + 4) + '" text-anchor="end" font-size="11" fill="#94a3b8">'
                + fmt(gv) + '</text>';
        }

        // 折线下方的渐变面积
        if (n > 1) {
            var area = 'M' + x(0) + ' ' + y(data[0].value);
            for (var a = 1; a < n; a++) {
                area += ' L' + x(a) + ' ' + y(data[a].value);
            }
            area += ' L' + x(n - 1) + ' ' + (T + ih) + ' L' + x(0) + ' ' + (T + ih) + ' Z';
            out += '<path d="' + area + '" fill="url(#mcGrad)"/>';
        }

        // 折线
        var path = '';
        for (var i = 0; i < n; i++) {
            path += (i === 0 ? 'M' : 'L') + x(i) + ' ' + y(data[i].value) + ' ';
        }
        out += '<path d="' + path + '" fill="none" stroke="' + color
            + '" stroke-width="2.5" stroke-linejoin="round" stroke-linecap="round"/>';

        // 数据点 / 数值 / X 轴标签
        for (var k = 0; k < n; k++) {
            var px = x(k), py = y(data[k].value);
            out += '<circle cx="' + px + '" cy="' + py + '" r="3.6" fill="#fff" stroke="' + color + '" stroke-width="2"/>';
            out += '<text x="' + px + '" y="' + (py - 10) + '" text-anchor="middle" font-size="11" fill="#475569">'
                + fmt(data[k].value) + '</text>';
            out += '<text x="' + px + '" y="' + (H - 8) + '" text-anchor="middle" font-size="11" fill="#94a3b8">'
                + esc(data[k].label) + '</text>';
        }

        el.innerHTML = '<svg viewBox="0 0 ' + W + ' ' + H + '" style="width:100%;height:auto;display:block">'
            + '<defs><linearGradient id="mcGrad" x1="0" y1="0" x2="0" y2="1">'
            + '<stop offset="0%" stop-color="' + color + '" stop-opacity="0.26"/>'
            + '<stop offset="100%" stop-color="' + color + '" stop-opacity="0.02"/>'
            + '</linearGradient></defs>' + out + '</svg>';
    }

    /**
     * 环形图
     * @param el    容器
     * @param parts [{label, value, color}, ...]
     * @param opt   {center: '个车位'}
     */
    function donut(el, parts, opt) {
        opt = opt || {};
        parts = parts || [];
        if (!el) {
            return;
        }
        var total = 0;
        parts.forEach(function (p) { total += (p.value || 0); });

        var size = 180, r = 62, sw = 22;
        var cx = size / 2, cy = size / 2;
        var circumference = 2 * Math.PI * r;

        var out = '<circle cx="' + cx + '" cy="' + cy + '" r="' + r
            + '" fill="none" stroke="#eef2f7" stroke-width="' + sw + '"/>';

        var offset = 0;
        parts.forEach(function (p) {
            var v = p.value || 0;
            if (v <= 0 || total <= 0) {
                return;
            }
            var len = circumference * v / total;
            out += '<circle cx="' + cx + '" cy="' + cy + '" r="' + r + '" fill="none" stroke="' + p.color
                + '" stroke-width="' + sw + '" stroke-dasharray="' + len + ' ' + (circumference - len)
                + '" stroke-dashoffset="' + (-offset) + '" transform="rotate(-90 ' + cx + ' ' + cy + ')">'
                + '<title>' + esc(p.label) + ' ' + v + '</title></circle>';
            offset += len;
        });

        out += '<text x="' + cx + '" y="' + (cy - 2) + '" text-anchor="middle" font-size="24" font-weight="700" fill="#1f2937">'
            + total + '</text>';
        out += '<text x="' + cx + '" y="' + (cy + 18) + '" text-anchor="middle" font-size="11" fill="#94a3b8">'
            + esc(opt.center || '') + '</text>';

        el.innerHTML = '<svg viewBox="0 0 ' + size + ' ' + size
            + '" style="width:180px;height:180px;display:block;margin:0 auto">' + out + '</svg>';
    }

    /** 图例（带占比） */
    function legend(el, parts, unit) {
        if (!el) {
            return;
        }
        var total = 0;
        parts.forEach(function (p) { total += (p.value || 0); });
        el.innerHTML = parts.map(function (p) {
            var pct = total > 0 ? Math.round((p.value || 0) * 100 / total) : 0;
            return '<span><i style="background:' + p.color + '"></i>' + esc(p.label)
                + ' <b>' + (p.value || 0) + '</b>' + (unit || '') + ' <em>' + pct + '%</em></span>';
        }).join('');
    }

    global.MiniChart = {line: line, donut: donut, legend: legend};
})(window);
