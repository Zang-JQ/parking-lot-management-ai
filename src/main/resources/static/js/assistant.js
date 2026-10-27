/* ==========================================================================
   AI 业务助手 · 右下角悬浮对话窗
   用法：页面底部一行 <script src="../js/assistant.js"></script>
   自己注入样式和 DOM，不依赖 jQuery，也不改页面原有结构。
   ========================================================================== */
(function () {
    /* 顺手把这套页面的 toast 也装上：历史代码里的 alert(...) 会变成右下角提示 */
    (function loadToast() {
        if (window.toast) {
            return;
        }
        var el = document.createElement('script');
        var here = (document.currentScript && document.currentScript.src) || '';
        el.src = here ? here.replace(/assistant\.js.*$/, 'toast.js') : '../js/toast.js';
        document.head.appendChild(el);
    })();

    if (window.__aiAssistantReady) {
        return;
    }
    window.__aiAssistantReady = true;

    var username = new URLSearchParams(window.location.search).get('username') || '';

    /* ---------------- 样式 ---------------- */
    var css = ''
        + '#aiFab{position:fixed;right:26px;bottom:26px;width:56px;height:56px;border:none;border-radius:50%;'
        + 'background:linear-gradient(135deg,#0ea5e9,#0369a1);color:#fff;font-size:24px;line-height:56px;'
        + 'cursor:pointer;box-shadow:0 10px 26px -8px rgba(14,165,233,.75);z-index:9998;'
        + 'transition:transform .18s ease,box-shadow .18s ease;padding:0}'
        + '#aiFab:hover{transform:translateY(-2px) scale(1.04);box-shadow:0 14px 30px -8px rgba(14,165,233,.85)}'
        + '#aiFab::after{content:"";position:absolute;inset:0;border-radius:50%;'
        + 'box-shadow:0 0 0 0 rgba(14,165,233,.55);animation:aiPulse 2.4s ease-out infinite}'
        + '@keyframes aiPulse{0%{box-shadow:0 0 0 0 rgba(14,165,233,.5)}70%{box-shadow:0 0 0 14px rgba(14,165,233,0)}'
        + '100%{box-shadow:0 0 0 0 rgba(14,165,233,0)}}'
        + '#aiPanel{position:fixed;right:26px;bottom:96px;width:372px;max-width:calc(100vw - 32px);height:540px;'
        + 'max-height:calc(100vh - 140px);background:#fff;border-radius:16px;display:none;flex-direction:column;'
        + 'overflow:hidden;z-index:9999;border:1px solid #e6eaf2;'
        + 'box-shadow:0 24px 60px -20px rgba(16,24,40,.45);font-size:14px}'
        + '#aiPanel.open{display:flex;animation:aiIn .18s ease-out}'
        + '@keyframes aiIn{from{opacity:0;transform:translateY(10px)}to{opacity:1;transform:none}}'
        + '.ai-head{display:flex;align-items:center;justify-content:space-between;padding:14px 16px;'
        + 'background:linear-gradient(135deg,#0ea5e9,#0369a1);color:#fff;flex:none}'
        + '.ai-title{font-size:15px;font-weight:700;letter-spacing:.3px}'
        + '.ai-sub{font-size:12px;opacity:.85;margin-top:2px}'
        + '.ai-actions{display:flex;gap:6px;align-items:center}'
        + '.ai-close,.ai-clear{background:rgba(255,255,255,.18);border:none;color:#fff;width:26px;height:26px;'
        + 'border-radius:50%;cursor:pointer;font-size:13px;line-height:1;padding:0}'
        + '.ai-close:hover,.ai-clear:hover{background:rgba(255,255,255,.32)}'
        + '.ai-body{flex:1;overflow-y:auto;padding:14px;background:#f7f9fc;display:flex;flex-direction:column;gap:10px}'
        + '.ai-msg{max-width:86%;padding:9px 12px;border-radius:12px;line-height:1.6;word-break:break-word;white-space:pre-wrap}'
        + '.ai-msg.bot{background:#fff;border:1px solid #e6eaf2;color:#1f2937;border-bottom-left-radius:4px;align-self:flex-start}'
        + '.ai-msg.me{background:#0ea5e9;color:#fff;border-bottom-right-radius:4px;align-self:flex-end}'
        + '.ai-msg.err{background:#fef2f2;border-color:#fecaca;color:#b91c1c}'
        + '.ai-typing{align-self:flex-start;background:#fff;border:1px solid #e6eaf2;border-radius:12px;'
        + 'padding:10px 14px;display:flex;gap:4px}'
        + '.ai-typing i{width:6px;height:6px;border-radius:50%;background:#94a3b8;animation:aiBlink 1.2s infinite}'
        + '.ai-typing i:nth-child(2){animation-delay:.2s}'
        + '.ai-typing i:nth-child(3){animation-delay:.4s}'
        + '@keyframes aiBlink{0%,80%,100%{opacity:.25}40%{opacity:1}}'
        + '.ai-quick{flex:none;display:flex;gap:6px;padding:8px 12px 0;flex-wrap:wrap;background:#fff;'
        + 'border-top:1px solid #f0f3f8}'
        + '.ai-quick button{border:1px solid #dbeafe;background:#eff6ff;color:#0369a1;font-size:12px;'
        + 'padding:5px 10px;border-radius:12px;cursor:pointer}'
        + '.ai-quick button:hover{background:#dbeafe}'
        + '.ai-foot{flex:none;display:flex;gap:8px;padding:10px 12px 12px;background:#fff}'
        + '#aiInput{flex:1;min-width:0;border:1px solid #e2e8f0;border-radius:10px;padding:9px 12px;font-size:14px;'
        + 'font-family:inherit;outline:none}'
        + '#aiInput:focus{border-color:#0ea5e9;box-shadow:0 0 0 3px rgba(14,165,233,.12)}'
        + '#aiSend{border:none;border-radius:10px;background:#0ea5e9;color:#fff;padding:0 16px;font-size:14px;'
        + 'font-weight:600;cursor:pointer;flex:none}'
        + '#aiSend:hover{background:#0284c7}'
        + '#aiSend:disabled{opacity:.55;cursor:default}';

    var style = document.createElement('style');
    style.textContent = css;
    document.head.appendChild(style);

    /* ---------------- DOM ---------------- */
    var fab = document.createElement('button');
    fab.id = 'aiFab';
    fab.type = 'button';
    fab.title = 'AI 助手';
    fab.textContent = '🤖';

    var panel = document.createElement('div');
    panel.id = 'aiPanel';
    panel.innerHTML =
        '<div class="ai-head">'
        + '  <div><div class="ai-title">AI 助手</div><div class="ai-sub">车位 · 费用 · 会员，直接问我</div></div>'
        + '  <div class="ai-actions">'
        + '    <button class="ai-clear" type="button" title="清空对话">🗑</button>'
        + '    <button class="ai-close" type="button" title="收起">✕</button>'
        + '  </div>'
        + '</div>'
        + '<div class="ai-body" id="aiBody"></div>'
        + '<div class="ai-quick" id="aiQuick">'
        + '  <button type="button">现在还有几个空位？</button>'
        + '  <button type="button">我停车要多少钱？</button>'
        + '  <button type="button">我的会员什么时候到期？</button>'
        + '</div>'
        + '<div class="ai-foot">'
        + '  <input id="aiInput" type="text" placeholder="输入问题，回车发送" maxlength="500">'
        + '  <button id="aiSend" type="button">发送</button>'
        + '</div>';

    document.body.appendChild(fab);
    document.body.appendChild(panel);

    var body = panel.querySelector('#aiBody');
    var quick = panel.querySelector('#aiQuick');
    var input = panel.querySelector('#aiInput');
    var sendBtn = panel.querySelector('#aiSend');
    var busy = false;
    var greeted = false;

    /* ---------------- 交互逻辑 ---------------- */
    function addMsg(who, text, isError) {
        var div = document.createElement('div');
        div.className = 'ai-msg ' + who + (isError ? ' err' : '');
        div.textContent = text;          // 用 textContent，天然防 XSS
        body.appendChild(div);
        body.scrollTop = body.scrollHeight;
        return div;
    }

    function addTyping() {
        var div = document.createElement('div');
        div.className = 'ai-typing';
        div.innerHTML = '<i></i><i></i><i></i>';
        body.appendChild(div);
        body.scrollTop = body.scrollHeight;
        return div;
    }

    function setBusy(flag) {
        busy = flag;
        sendBtn.disabled = flag;
        sendBtn.textContent = flag ? '…' : '发送';
    }

    function send(text) {
        text = (text || '').trim();
        if (!text || busy) {
            return;
        }
        setBusy(true);
        addMsg('me', text);
        input.value = '';
        var typing = addTyping();

        fetch('Ai/chat', {
            method: 'POST',
            headers: {'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8'},
            body: 'message=' + encodeURIComponent(text) + '&username=' + encodeURIComponent(username)
        })
            .then(function (r) { return r.json(); })
            .then(function (res) {
                typing.remove();
                if (res && res.success) {
                    addMsg('bot', res.answer);
                } else {
                    addMsg('bot', (res && res.message) || '助手暂时不可用，请稍后再试', true);
                }
            })
            .catch(function () {
                typing.remove();
                addMsg('bot', '网络异常，没连上助手，请稍后再试', true);
            })
            .then(function () {
                setBusy(false);
                body.scrollTop = body.scrollHeight;
            });
    }

    var greetingText = '你好，我是停车场 AI 助手。可以直接问我「还有几个空位」「我停了多久要交多少钱」「会员什么时候到期」。';

    function addGreeting() {
        addMsg('bot', greetingText);
    }

    /** 打开时从服务端拉一次历史，刷新页面后也能接着看（服务端按 userId 存记忆） */
    function loadHistory() {
        return fetch('Ai/history?username=' + encodeURIComponent(username))
            .then(function (r) { return r.json(); })
            .then(function (items) {
                if (!Array.isArray(items) || items.length === 0) {
                    return false;
                }
                items.forEach(function (m) {
                    addMsg(m.role === 'user' ? 'me' : 'bot', m.text);
                });
                return true;
            })
            .catch(function () { return false; });
    }

    function toggle(open) {
        panel.classList.toggle('open', open);
        if (open && !greeted) {
            greeted = true;
            input.focus();
            if (body.children.length > 0) {
                return;   // 本次页面已经渲染过内容
            }
            loadHistory().then(function (hasHistory) {
                if (!hasHistory) {
                    addGreeting();
                }
            });
        }
    }

    /** 清空服务端记忆 + 本地消息 */
    function clearChat() {
        fetch('Ai/clear', {
            method: 'POST',
            headers: {'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8'},
            body: 'username=' + encodeURIComponent(username)
        }).then(function () {
            body.innerHTML = '';
            addGreeting();
        }).catch(function () {
            addMsg('bot', '清空失败，请检查网络', true);
        });
    }

    fab.addEventListener('click', function () { toggle(!panel.classList.contains('open')); });
    panel.querySelector('.ai-close').addEventListener('click', function () { toggle(false); });
    panel.querySelector('.ai-clear').addEventListener('click', clearChat);
    sendBtn.addEventListener('click', function () { send(input.value); });
    input.addEventListener('keydown', function (e) {
        if (e.key === 'Enter') {
            e.preventDefault();
            send(input.value);
        }
    });
    quick.addEventListener('click', function (e) {
        if (e.target.tagName === 'BUTTON') {
            send(e.target.textContent);
        }
    });

    // 支持 xxx.html?username=feifei#ai 直接展开助手（演示 / 截图方便）
    if (window.location.hash === '#ai') {
        toggle(true);
    }
})();
