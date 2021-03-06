console.log("LOAD JS SOURCE theme.js");
const BACK_ACTION = "0";
const REFRESH_ACTION = "1";
const NORMAL_ACTION = "2";
window.loadAction = NORMAL_ACTION;
window.loadScrollY = 0;
var anchorElem, elemToActivation;
var corrector;


function setLoadAction(loadAction) {
    console.log("setLoadAction " + loadAction);
    window.loadAction = loadAction;
}

function setLoadScrollY(loadScrollY) {
    console.log("setLoadScrollY " + loadScrollY);
    window.loadScrollY = Number(loadScrollY);
}

function disableImages() {
    var images = document.querySelectorAll(".linked-image");
    console.log(images);
    for (var i = 0; i < images.length; i++) {
        var image = images[i];
        var src = image.getAttribute("src");
        image.removeAttribute("src");
        image.setAttribute("data-src", src);
    }
}

//Вызывается при обновлении прогресса загрузке страницы и при загрузке её ресурсов
//По идеи должна верно скроллить к нужному элементу, даже если пользователь прокрутил страницу
//Как оно работает и работает ли вообще - объяснить не могу
function onProgressChanged() {
    if (corrector)
        corrector.startObserver();
}

function getScrollTop() {
    return (window.pageYOffset || document.documentElement.scrollTop) - (document.documentElement.clientTop || 0);
}
//name может быть EventObject или строкок
//name это аттрибут тега html, может быть просто якорем или entry+post_id
//Вызывается из джавы, если находится на той-же странице, и в ссылке есть entry или якорь, а также при загрузке страницы
//PageInfo.elemToScroll - переменная, заданная в шаблоне в теге script, содержит в себе якорь или entry



function scrollToElement(name) {
    console.log("scrollToElement " + name);
    if (typeof name != 'string') {
        name = PageInfo.elemToScroll;
    }
    var anchorData = /([^-]*)-([\d]*)-(\d+)/g.exec(name);
    if (anchorData) {
        //anchorData[1] - name (spoil, quote, etc)
        //anchorData[2] - post id
        //anchorData[3] - number block of post, begin with 1
        anchorData[1] = anchorData[1].toLowerCase();
        if (anchorData[1] === "spoiler") anchorData[1] = "spoil";
        if (anchorData[1] === "hide") anchorData[1] = "hidden";
        anchorElem = document.querySelector('[name="entry' + anchorData[2] + '"]');
        anchorElem = anchorElem.querySelectorAll(".post-block." + anchorData[1])[Number(anchorData[3]) - 1];
    } else {
        anchorElem = document.querySelector('[name="' + name + '"]');
    }
    if (anchorElem) {
        //Открытие всех спойлеров
        var block = anchorElem;
        while (block.classList && !block.classList.contains('post_body')) {
            /*if (block.classList.contains('spoil')) {
                block.classList.remove('close');
                block.classList.add('open');
            }*/
            toggler("close", "open", block);
            block = block.parentNode;
        }
        //Открытие шапки
        block = anchorElem;
        while (block.classList && !block.classList.contains('post_container')) {
            block = block.parentNode;
        }
        if (block.classList.contains("close")) {
            var button = block.querySelector(".hat_button");
            toggleButton(button, "hat_content");
        }
    } else {
        anchorElem = document.documentElement;
    }
    console.log("ANCHOR " + name);
    console.log("loadAction " + window.loadAction);
    console.log("loadScrollY " + window.loadScrollY);
    if (window.loadAction == BACK_ACTION || window.loadAction == REFRESH_ACTION) {
        setTimeout(function () {
            window.scrollTo(0, window.loadScrollY);
        }, 1);
        nativeEvents.addEventListener(nativeEvents.PAGE, function () {
            //setTimeout(function () {
                window.scrollTo(0, window.loadScrollY);
            //}, 1);
        });
    } else if (window.loadAction == NORMAL_ACTION) {
        setTimeout(function () {
            doScroll(anchorElem);
        }, 1);
        nativeEvents.addEventListener(nativeEvents.PAGE, function () {
            //setTimeout(function () {
                doScroll(anchorElem);
            //}, 1);
        });
    }
}

function doScroll(tAnchorElem) {
    try {
        tAnchorElem.focus();
        var access_anchor = tAnchorElem.querySelector(".accessibility_anchor");
        if (access_anchor) {
            access_anchor.focus();
        }
    } catch (ex) {
        console.error(ex);
    }

    tAnchorElem.scrollIntoView();

    //Активация элементов, убирается класс active с уже активированных
    if (elemToActivation)
        elemToActivation.classList.remove('active');

    var postElem = tAnchorElem;
    console.log(postElem);
    while (postElem && !postElem.classList.contains("post_container")) {
        postElem = postElem.parentElement;
        console.log(postElem);
    }
    elemToActivation = postElem;
    if (elemToActivation)
        elemToActivation.classList.add('active');
}

function selectionToQuote() {
    var selObj = window.getSelection();
    var selectedText = selObj.toString();


    var p = selObj.anchorNode.parentNode;
    while (p.classList && !p.classList.contains('post_container')) {
        p = p.parentNode;
    }
    if (typeof p === "undefined" || typeof p.dataset === "undefined") {
        ITheme.toast("Для этого действия необходимо выбрать текст сообщения");
        return;
    }
    var postId = p.dataset.postId;
    if (selectedText != null && postId != null) {
        ITheme.quotePost(selectedText, "" + postId);
    } else {
        ITheme.toast("Ошибка создания цитаты: [" + selectedText + ", " + postId + "]");
        return;
    }
}

function copySelectedText() {
    var selectedText = window.getSelection().toString();
    if (selectedText != null && selectedText) {
        ITheme.copySelectedText(selectedText);
    }
}

function shareSelectedText() {
    var selectedText = window.getSelection().toString();
    if (selectedText != null && selectedText) {
        ITheme.shareSelectedText(selectedText);
    }
}


function selectAllPostText() {
    var selObj = window.getSelection();
    var p = selObj.anchorNode.parentNode;
    while (p.classList && !p.classList.contains('post_body')) {
        p = p.parentNode;
    }
    if (typeof p.classList === "undefined" || !p.classList.contains('post_body')) {
        ITheme.toast("Для этого действия необходимо выбрать текст сообщения");
        return;
    }
    var rng, sel;
    if (document.createRange) {
        rng = document.createRange();
        rng.selectNode(p);
        sel = window.getSelection();
        sel.removeAllRanges();
        sel.addRange(rng);
    } else {
        rng = document.body.createTextRange();
        rng.moveToElementText(p);
        rng.select();
    }
}

function ScrollCorrector() {
    console.log("Scroll Corrector initialized");
    var postElements = document.querySelectorAll(".post_container");
    var visibleElements = [];
    var visibleElement = anchorElem;
    var lastPosition = 0;
    var frames = 60 * 1;
    var frame = 0;
    var observerId = 0;

    for (var i = 0; i < postElements.length; i++) {
        postElements[i].addEventListener("mousedown", downEvent);
        postElements[i].addEventListener("touchdown", downEvent);
    }

    function downEvent(e) {

        var elem = e.target;
        while (!elem.classList.contains("post_container")) {
            elem = elem.parentElement;
        }
        visibleElement = elem;
        updateLastPosition();
    }

    this.startObserver = function () {
        startObserver();
    }

    window.addEventListener("scroll", function () {
        setVisible();
        updateLastPosition();
        frame = 0;
    });

    function updateLastPosition() {
        lastPosition = getCoordinates(visibleElement).top;
        //console.log("Update LastPosition: " + lastPosition);
    }

    function tryScroll() {

        var delta = getCoordinates(visibleElement).top - lastPosition;
        if (delta == 0)
            return;
        /*for (var i = 0; i < visibleElements.length; i++) {
            var elem = visibleElements[i];
            console.log("Elem [" + i + "]: " + getCoordinates(elem).top);
        }*/
        console.log("Scroll by delta: " + delta + ", lastPosition: " + lastPosition + ", visElemTop: " + getCoordinates(visibleElement).top);
        window.scrollBy(0, delta);
        updateLastPosition();
        frame = 0;
    }

    function startObserver() {
        if (observerId == 1) {
            return;
        }
        setVisible();
        console.log("Start Scroll Observer");

        function observerLoop() {
            tryScroll();
            if (frame < frames) {
                requestAnimationFrame(observerLoop);
                frame++;
            } else {
                cancelAnimationFrame(observerLoop);
                observerId = 0;
                frame = 0;
                console.log("Stop Scroll Observer");
            }
        }
        observerId = 1;
        observerLoop();
    }

    /*function setVisible(newVisible){
        if (visibleElement) {
            visibleElement.style.opacity = 1;
        }
        visibleElement = newVisible;
        visibleElement.style.opacity = 0.5;
    }*/

    function setVisible() {
        return;
        visibleElements = getVisiblePosts();
        if (visibleElement) {
            visibleElement.style.opacity = 1;
        }
        /*if (visibleElements.length > 0) {
            visibleElement = getNearest(visibleElements);
        }*/
        visibleElement = getNearest(visibleElements);
        visibleElement.style.opacity = 0.5;
    }

    function getVisiblePosts() {
        var scrollTop = getScrollTop();
        var windowHeight = document.documentElement.clientHeight;
        if (!visibleElement)
            visibleElements = [];
        visibleElements.length = 0;
        for (var i = 0; i < postElements.length; i++) {
            var el = postElements[i];
            if (el.offsetHeight + el.offsetTop < scrollTop || el.offsetTop > scrollTop + windowHeight)
                continue;
            visibleElements.push(el);
        }
        return visibleElements;
    }

    function getNearest(visibleElements) {
        var scrollTop = getScrollTop();
        var windowHeight = document.documentElement.clientHeight;
        var nearest = visibleElements[0];
        var deltaHeight = windowHeight;
        var delta = 0;
        for (var i = 0; i < visibleElements.length; i++) {
            var el = visibleElements[i];
            var bottomY = Math.abs(el.offsetTop + el.offsetHeight - scrollTop - windowHeight);
            if (deltaHeight - bottomY < delta) {
                break;
            }
            delta = deltaHeight - bottomY;
            deltaHeight = bottomY;
            nearest = el;
        }
        return nearest;
    }
}

function initScrollCorrector() {
    corrector = new ScrollCorrector();
}

function transformAnchor() {
    var anchors = [];
    var links = document.querySelectorAll(".post_container .post_body a[name][title]");
    for (var i = 0; i < links.length; i++) {
        if (links[i].innerHTML === "ˇ") {
            anchors.push(links[i]);
        }
    }

    for (var i = 0; i < anchors.length; i++) {
        var item = anchors[i];
        item.classList.add("anchor");
        item.innerHTML = "";
        item.addEventListener("click", function (event) {
            var t = event.target;
            while (!t.classList.contains('post_container')) {
                t = t.parentElement;
            }
            ITheme.anchorDialog(t.dataset.postId, event.target.name);
        });
    }
}

nativeEvents.addEventListener(nativeEvents.DOM, transformAnchor);
nativeEvents.addEventListener(nativeEvents.DOM, initScrollCorrector);
nativeEvents.addEventListener(nativeEvents.DOM, scrollToElement);
