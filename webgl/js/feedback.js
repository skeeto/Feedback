function Feedback(canvas) {
    /* Init WebGL */
    var igloo = this.igloo = new Igloo(canvas);
    var gl = igloo.gl;
    gl.disable(gl.DEPTH_TEST);
    gl.enable(gl.BLEND);
    gl.blendFunc(gl.ONE, gl.ONE);
    gl.blendEquation(gl.FUNC_ADD);

    /* Defaults */
    this._scale = 0.98;
    this._rotate = 2.5;
    this._affine = null;
    this.setAffine();

    /* User poking */
    this.pointer = 0.1;
    this.colorspeed = 0.01;
    this.mousecolor = Feedback.randomColor();
    this.mouse = [0, 0];

    this.buffers = {
        quad: igloo.array(Igloo.QUAD2)
    };
    this.programs = {
        step:   igloo.program('glsl/quad.vert', 'glsl/feedback.frag'),
        circle: igloo.program('glsl/quad.vert', 'glsl/circle.frag'),
        square: igloo.program('glsl/quad.vert', 'glsl/square.frag')
    };
    this.textures = {
        state: igloo.texture().blank(canvas.width, canvas.height)
    };

    var _this = this;
    $(canvas).on('mousemove', function(event) {
        var $target = $(event.target),
            offset = $target.offset(),
            border = 1,
            x = event.pageX - offset.left - border,
            y = $target.height() - (event.pageY - offset.top - border);
        _this.mouse = [
            x / $target.width() * 2 - 1,
            y / $target.height() * 2 - 1
        ];
    });

    this.running = false;
    this.last = 0;
    this.delay = 0;
}

Feedback.IDENTITY3 = mat3.create();

Feedback.affine = function(tx, ty, sx, sy, a) {
    var m  = mat3.create();
    mat3.translate(m, m, [tx, ty]);
    mat3.rotate(m, m, a);
    mat3.scale(m, m, [sx, sy]);
    return m;
};

Feedback.randomColor = function() {
    var r = RNG.$;
    return [r.uniform(), r.uniform(), r.uniform(), r.uniform() * 0.5 + 0.5];
};

Feedback.perturb = function(color, rate) {
    for (var i = 0; i < 4; i++) {
        color[i] += RNG.$.normal() * rate;
        color[i] = Math.min(Math.max(color[i], 0), 1);
    }
    color[3] = Math.max(color[3], 0.5);
};

Feedback.prototype.setAffine = function() {
    var s = 1 / this._scale;
    this._affine = Feedback.affine(0, 0, s, s, this._rotate);
    return this;
};

Feedback.prototype.draw = function() {
    var gl = this.igloo.gl, w = gl.canvas.width, h = gl.canvas.height;
    this.textures.state.bind(0);
    this.programs.step.use()
        .attrib('quad', this.buffers.quad, 2)
        .uniformi('state', 0)
        .matrix('placement', Feedback.IDENTITY3)
        .matrix('transform', this._affine)
        .draw(gl.TRIANGLE_STRIP, Igloo.QUAD2.length / 2);
    this.fill('circle', this.mousecolor, this.mouse[0], this.mouse[1],
              this.pointer, this.pointer, 0);
    Feedback.perturb(this.mousecolor, this.colorspeed);
    if (RNG.$.random(5) === 0) this.disturb();
    this.textures.state.copy(0, 0, w, h);
    return this;
};

Feedback.prototype.disturb = function() {
    var type = RNG.$.uniform() < 0.5 ? 'square' : 'circle',
        color = Feedback.randomColor(),
        x = RNG.$.uniform() * 2 - 1,
        y = RNG.$.uniform() * 2 - 1,
        s = RNG.$.normal() * 0.16,
        a = RNG.$.uniform() * Math.PI * 2;
    color[3] *= 0.25;
    this.fill(type, color, x, y, s, s, a);
};

Feedback.prototype.fill = function(type, color, tx, ty, sx, sy, a) {
    var gl = this.igloo.gl;
    this.programs[type].use()
        .attrib('quad', this.buffers.quad, 2)
        .uniform('color', color)
        .matrix('placement', Feedback.affine(tx, ty, sx, sy, a))
        .matrix('transform', this._affine)
        .draw(gl.TRIANGLE_STRIP, Igloo.QUAD2.length / 2);
};

Feedback.prototype.frame = function() {
    var _this = this;
    window.requestAnimationFrame(function() {
        if (Date.now() - _this.last > _this.delay) {
            _this.draw();
            _this.last = Date.now();
        }
        if (_this.running) _this.frame();
    });
};

Feedback.prototype.start = function() {
    if (this.running == false) {
        this.running = true;
        this.frame();
    }
    return this;
};

Feedback.prototype.stop = function() {
    this.running = false;
    return this;
};

var feedback = null;
$(document).ready(function() {
    feedback = new Feedback($('#display')[0]);
    feedback.draw().start();
});
