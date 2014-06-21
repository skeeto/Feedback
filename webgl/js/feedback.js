/**
 * @param {HTMLCanvasElement} canvas
 * @constructor
 */
function Feedback(canvas) {
    /* Init WebGL */
    var igloo = this.igloo = new Igloo(canvas);
    var gl = igloo.gl;
    gl.disable(gl.DEPTH_TEST);
    gl.enable(gl.BLEND);
    gl.blendEquation(gl.FUNC_ADD);
    gl.blendFunc(gl.SRC_ALPHA, gl.SRC_ALPHA);

    /* Defaults */
    this._gravity = 0.98;
    this._rotate = 2.5;
    this._affine = null;
    this.setAffine();

    /* User poking */
    this.pointer = {
        position: null,
        size: 0.1,
        angle: 0,
        spin: 0.05,
        type: 0,
        color: Feedback.randomColor(),
        colorspeed: 0.01
    };
    this.noise = true;

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
    this.framebuffers = {
        access: igloo.framebuffer(this.textures.state)
    };
    this.igloo.defaultFramebuffer.bind();

    var _this = this;
    $(canvas).on('mousemove', function(event) {
        var $target = $(event.target),
            offset = $target.offset(),
            border = 1,
            x = event.pageX - offset.left - border,
            y = $target.height() - (event.pageY - offset.top - border);
        _this.pointer.position = [
            x / $target.width() * 2 - 1,
            y / $target.height() * 2 - 1
        ];
    });
    $(canvas).on('mouseout', function() {
        _this.pointer.position = null;
    });

    this.running = false;
    this.last = 0;
    this.delay = 0;
}

/**
 * @type {Float32Array}
 * @constant
 */
Feedback.IDENTITY3 = mat3.create();

/**
 * @type {Array}
 * @constant
 */
Feedback.SHAPES = ['circle', 'square'];

/**
 * @param {number} tx translation x
 * @param {number} ty translation y
 * @param {number} sx scale x
 * @param {number} sy scale y
 * @param {number} a rotation angle (radians)
 * @returns {Float32Array} a mat3 affine transformation
 */
Feedback.affine = function(tx, ty, sx, sy, a) {
    var m  = mat3.create();
    mat3.translate(m, m, [tx, ty]);
    mat3.rotate(m, m, a);
    mat3.scale(m, m, [sx, sy]);
    return m;
};

/**
 * @returns {Float32Array}
 */
Feedback.randomColor = function() {
    var r = RNG.$;
    return [r.uniform(), r.uniform(), r.uniform(), r.uniform() * 0.5 + 0.5];
};

/**
 * Slightly perturb a color into a different color.
 * @param {Float32Array} color
 * @param {number} rate
 * @returns {Float32Array} the modified color
 */
Feedback.perturb = function(color, rate) {
    for (var i = 0; i < 4; i++) {
        color[i] += RNG.$.normal() * rate;
        color[i] = Math.min(Math.max(color[i], 0), 1);
    }
    color[3] = Math.max(color[3], 0.5);
    return color;
};

/**
 * Update the affine transformation.
 * @returns {Feedback} this
 */
Feedback.prototype.setAffine = function() {
    var s = 1 / this._gravity;
    this._affine = Feedback.affine(0, 0, s, s, this._rotate);
    return this;
};

/**
 * Adjusts the feedback transformation.
 * @param {string} value one of gravity or rotate
 * @param {number} factor to be multiplied against the value
 * @returns {Feedback} this
 */
Feedback.prototype.adjust = function(value, factor) {
    this['_' + value] *= factor;
    this.setAffine();
    return this;
};

/**
 * Step the feedback simulation and draw it to the screen.
 * @returns {Feedback} this
 */
Feedback.prototype.draw = function() {
    var gl = this.igloo.gl, w = gl.canvas.width, h = gl.canvas.height;
    this.textures.state.bind(0);
    this.programs.step.use()
        .attrib('quad', this.buffers.quad, 2)
        .uniformi('state', 0)
        .matrix('placement', Feedback.IDENTITY3)
        .matrix('transform', Feedback.IDENTITY3)
        .draw(gl.TRIANGLE_STRIP, Igloo.QUAD2.length / 2)
        .matrix('transform', this._affine)
        .draw(gl.TRIANGLE_STRIP, Igloo.QUAD2.length / 2);
    if (this.pointer.position != null) {
        console.log(this.pointer);
        this.fill(Feedback.SHAPES[this.pointer.type], this.pointer.color,
                  this.pointer.position[0], this.pointer.position[1],
                  this.pointer.size, this.pointer.size, this.pointer.angle);
    }
    if (this.noise && RNG.$.random(this.pointer == null ? 3 : 5) === 0) {
        this.disturb();
    }
    Feedback.perturb(this.pointer.color, this.pointer.colorspeed);
    this.pointer.angle += this.pointer.spin;
    this.textures.state.copy(0, 0, w, h);
    return this;
};

/**
 * Draw a single random shape on the display.
 * @returns {Feedback} this
 */
Feedback.prototype.disturb = function() {
    var type = RNG.$.uniform() < 0.5 ? 'square' : 'circle',
        color = Feedback.randomColor(),
        x = RNG.$.uniform() * 2 - 1,
        y = RNG.$.uniform() * 2 - 1,
        s = RNG.$.normal() * 0.16,
        a = RNG.$.uniform() * Math.PI * 2;
    color[3] = 1;
    this.fill(type, color, x, y, s, s, a);
    return this;
};

/**
 * Draw a specific shape to the display.
 * @param {string} type one of circle or square
 * @param {Float32Array} color
 * @param {number} tx translation x
 * @param {number} ty translation y
 * @param {number} sx scale x
 * @param {number} sx scale y
 * @param {number} a rotation angle (radians)
 * @returns {Feedback} this
 */
Feedback.prototype.fill = function(type, color, tx, ty, sx, sy, a) {
    var gl = this.igloo.gl;
    this.programs[type].use()
        .attrib('quad', this.buffers.quad, 2)
        .uniform('color', color)
        .matrix('placement', Feedback.affine(tx, ty, sx, sy, a))
        .matrix('transform', this._affine)
        .draw(gl.TRIANGLE_STRIP, Igloo.QUAD2.length / 2);
    return this;
};

/**
 * Get a data URL for the current display.
 * @returns {string}
 */
Feedback.prototype.toDataURL = function() {
    var gl = this.igloo.gl,
        w = gl.canvas.width,
        h = gl.canvas.height,
        rgba = new Uint8Array(w * h * 4);

    /* Gather image data from WebGL. */
    this.framebuffers.access.bind();
    gl.readPixels(0, 0, w, h, gl.RGBA, gl.UNSIGNED_BYTE, rgba);
    this.igloo.defaultFramebuffer.bind();

    /* Dump onto a background canvas for toDataURL(). Unfortunately
     * preserveDrawingBuffer doesn't allow me to fix the alpha, so I
     * can't use it. Plus it interferes with drawing. */
    var canvas = document.createElement('canvas');
    canvas.width = w; canvas.height = h;
    var ctx = canvas.getContext('2d'),
        image = ctx.getImageData(0, 0, w, h);
    for (var i = 0; i < rgba.length; i += 4) {
        image.data[i + 0] = rgba[i + 0];
        image.data[i + 1] = rgba[i + 1];
        image.data[i + 2] = rgba[i + 2];
        image.data[i + 3] = 255;
    }
    ctx.putImageData(image, 0, 0);
    return canvas.toDataURL();
};

/**
 * Clear all colors from the display.
 * @returns {Feedback} this
 */
Feedback.prototype.clear = function() {
    var gl = this.igloo.gl;
    this.framebuffers.access.bind();
    gl.clearColor(0, 0, 0, 1);
    gl.clear(gl.COLOR_BUFFER_BIT);
    this.igloo.defaultFramebuffer.bind();
    return this;
};

/**
 * Handle a single animation frame and register for the next one.
 * @returns {Feedback} this
 */
Feedback.prototype.frame = function() {
    var _this = this;
    window.requestAnimationFrame(function() {
        if (_this.delay === 0 || Date.now() - _this.last > _this.delay) {
            _this.draw();
            _this.last = Date.now();
        }
        if (_this.running) _this.frame();
    });
};

/**
 * Begin animating.
 * @returns {Feedback} this
 */
Feedback.prototype.start = function() {
    if (this.running == false) {
        $(this.igloo.canvas).css('cursor', 'none');
        this.running = true;
        this.frame();
    }
    return this;
};

/**
 * Stop animating.
 * @returns {Feedback} this
 */
Feedback.prototype.stop = function() {
    this.running = false;
    $(this.igloo.canvas).css('cursor', '');
    return this;
};

/**
 * Toggle animation.
 * @returns {Feedback} this
 */
Feedback.prototype.toggle = function() {
    if (this.running) {
        this.stop();
    } else {
        this.start();
    }
    return this;
};
