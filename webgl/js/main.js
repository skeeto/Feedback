var feedback = null;
$(document).ready(function() {
    feedback = new Feedback($('#display')[0]);
    feedback.draw().start();
    $(document).on('keyup', function(event) {
        switch (event.which) {
        case 82: /* r */
            feedback.adjust('rotate', event.shiftKey ? 1.01 : 0.99099);
            break;
        case 71: /* g */
            feedback.adjust('gravity', event.shiftKey ? 1.01 : 0.99099);
            break;
        case 78: /* n */
            feedback.noise = !feedback.noise;
            break;
        case 67: /* c */
            feedback.clear();
            break;
        case 32: /* [space] */
            feedback.toggle();
            break;
        case 84: /* t */
            var p = feedback.pointer;
            p.type = (p.type + 1) % Feedback.SHAPES.length;
            break;
        case 83: /* s */
            var url = feedback.toDataURL();
            Download.show(url);
            break;
        };
    });
});

/* Don't scroll on spacebar. */
$(window).on('keydown', function(event) {
    return !(event.keyCode === 32);
});

if (window.requestAnimationFrame == null) {
    window.requestAnimationFrame =
        window.webkitRequestAnimationFrame ||
        window.mozRequestAnimationFrame    ||
        function(callback){
            window.setTimeout(callback, 1000 / 60);
        };
}
