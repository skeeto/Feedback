function Feedback(canvas) {
    /* Init WebGL */
    var igloo = this.igloo = new Igloo(canvas);
    var gl = igloo.gl;
    gl.disable(gl.DEPTH_TEST);
    gl.enable(gl.BLEND);
    gl.blendFunc(gl.SRC_ALPHA, gl.ONE_MINUS_SRC_ALPHA);

    /* Defaults */
    this._scale = 0.99;
    this._rotate = 0.1;//2.5;
    this._affine = null;
    this.setAffine();

    /* User poking */
    this.pointer = 50;
    this.colorspeed = 15;

    this.buffers = {
        quad: igloo.array(Igloo.QUAD2)
    };
    this.programs = {
        step: igloo.program('glsl/quad.vert', 'glsl/feedback.frag')
    };
    this.textures = {
        state: igloo.texture($('#init')[0])//.blank(canvas.width, canvas.height)
    };

    this.timer = null;
}

Feedback.prototype.setAffine = function() {
    var m = this._affine = mat3.create();
    mat3.rotate(m, m, this._rotate);
    mat3.scale(m, m, [1 / this._scale, 1 / this._scale]);
    return this;
};

Feedback.prototype.draw = function() {
    var gl = this.igloo.gl, w = gl.canvas.width, h = gl.canvas.height;
    this.textures.state.bind(0);
    this.programs.step.use()
        .attrib('quad', this.buffers.quad, 2)
        .uniformi('state', 0)
        .matrix('transform', this._affine)
        .draw(gl.TRIANGLE_STRIP, Igloo.QUAD2.length / 2);
    this.textures.state.copy(0, 0, w, h);
    return this;
};

Feedback.prototype.start = function() {
    var _this = this;
    if (this.timer == null) {
        this.timer = window.setInterval(function() {
            _this.draw();
        }, 33);
    }
    return this;
};

Feedback.prototype.stop = function() {
    window.clearInterval(this.timer);
    this.timer = null;
    return this;
};

var feedback = null;
$(document).ready(function() {
    feedback = new Feedback($('#display')[0]);
    feedback.draw().start();
});
