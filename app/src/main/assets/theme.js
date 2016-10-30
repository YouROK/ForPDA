var anchorElem, elemToActivation;
var lastTop = 0,
    lastScrollTop = 0,
    lastDeltaTop = 0,
    lastDeltaScroll = 0;

//Вызывается при обновлении прогресса загрузке страницы и при загрузке её ресурсов
//По идеи должна верно скроллить к нужному элементу, даже если пользователь прокрутил страницу
//Как оно работает и работает ли вообще - объяснить не могу
function onProgressChanged() {
    console.log("call onprogress");
    var newTop = getCoordinates(anchorElem).top;
    var newScrollTop = window.pageYOffset || document.scrollTop;
    var tempDeltaTop = newTop - lastTop;
    var tempDeltaScroll = newScrollTop - lastScrollTop;

    var delta = tempDeltaTop;
    if (lastDeltaTop != 0)
        delta = delta + (lastDeltaTop - tempDeltaScroll + lastDeltaScroll);

    //console.log("prch: "+tempDeltaTop+"  :  "+tempDeltaScroll+"  :  "+lastDeltaTop+"  :  "+lastDeltaScroll+ "  :  "+delta);
    if (delta != 0) {
        window.scrollBy(0, delta);
    }

    lastTop = newTop;
    lastScrollTop = newScrollTop;
    lastDeltaTop = tempDeltaTop;
    lastDeltaScroll = tempDeltaScroll
}


//name может быть EventObject или строкок
//name это аттрибут тега html, может быть просто якорем или entry+post_id
//Вызывается из джавы, если находится на той-же странице, и в ссылке есть entry или якорь, а также при загрузке страницы
//PageInfo.elemToScroll - переменная, заданная в шаблоне в теге script, содержит в себе якорь или entry
//doOnLoadScroll - объект в window, задаётся false только когда была сделана перезагрузка страницы или переход назад
function scrollToElement(name) {
    console.log("call scroll to");
    if (typeof doOnLoadScroll !== "undefined")
        if (doOnLoadScroll == false)
            return;

    if (typeof name != 'string') name = PageInfo.elemToScroll;
    console.log("do scroll to " + name);
    anchorElem = document.querySelector('[name="' + name + '"]');
    var p = anchorElem;
    if (anchorElem) {
        while (p.classList && !p.classList.contains('post_body')) {
            if (p.classList.contains('spoil')) {
                p.classList.remove('close');
                p.classList.add('open');
            }
            p = p.parentNode;
        }
    }
    anchorElem.scrollIntoView();
    lastTop = getCoordinates(anchorElem).top;
    lastScrollTop = window.pageYOffset || document.scrollTop;


    //Активация элементов, убирается класс active с уже активированных
    if(elemToActivation)
        elemToActivation.classList.remove('active');

    elemToActivation = document.querySelector('.post_container[name="'+name+'"]');
    elemToActivation.classList.add('active');
}

document.addEventListener('DOMContentLoaded', scrollToElement);


/**
 *		===================
 *		blocks close & open
 *		===================
 */

function blocksOpenClose() {
    var blockTitleAll = document.querySelectorAll('.post-block.spoil>.block-title,.post-block.code>.block-title'),
        bt;

    if (!blockTitleAll[0]) return;

    for (var i = 0; i < blockTitleAll.length; i++) {
        bt = blockTitleAll[i];
        var bb = bt.parentElement.querySelector('.block-body');
        if (bb.parentElement.classList.contains('code') && bb.scrollHeight <= bb.offsetHeight) bb.parentElement.classList.remove('box');
        bt.addEventListener('click', clickOnElement, false);
    }

    function clickOnElement(event) {
        var p = el().t.parentElement;

        function el() {
            var event = event || window.event;
            var target = event.target || event.srcElement;
            return {
                e: event,
                t: target
            };
        }
        if (p.classList.contains('spoil')) toggler("close", "open");
        if (p.classList.contains('code')) toggler("unbox", "box");

        function toggler(c, o) {
            if (p.classList.contains(c)) {
                p.classList.remove(c);
                p.classList.add(o);
            } else if (p.classList.contains(o)) {
                p.classList.remove(o);
                p.classList.add(c);
            }
        }
    }
}

document.addEventListener('DOMContentLoaded', blocksOpenClose);

function getCoordinates(elem) {
    // (1)
    var box = elem.getBoundingClientRect();

    var body = document.body;
    var docEl = document.documentElement;

    // (2)
    var scrollTop = window.pageYOffset || docEl.scrollTop || body.scrollTop;
    var scrollLeft = window.pageXOffset || docEl.scrollLeft || body.scrollLeft;

    // (3)
    var clientTop = docEl.clientTop || body.clientTop || 0;
    var clientLeft = docEl.clientLeft || body.clientLeft || 0;

    // (4)
    var top = box.top + scrollTop - clientTop;
    var left = box.left + scrollLeft - clientLeft;

    return {
        top: top,
        left: left
    };
}

function selectionToQuote() {
    var selObj = window.getSelection();
    var selectedText = selObj.toString();


    var p = selObj.anchorNode.parentNode;
    while (p.classList && !p.classList.contains('post_container')) {
        p = p.parentNode;
    }
    var postId = p.dataset.postId;
    console.log(selectedText+" : "+postId);
    if (selectedText != null && selectedText != '' && postId != null && postId != ''){
        ITheme.quotePost(selectedText, postId);
    }
}