#ifdef GL_ES
precision mediump float;
#endif

uniform sampler2D state;
uniform mat3 transform;

varying vec3 coord;

void main() {
    vec2 p = (transform * coord).st / 2.0 + 0.5;
    if (p.s < 0.0 || p.s > 1.0 || p.t < 0.0 || p.t > 1.0) {
        gl_FragColor = vec4(0.0, 0.0, 0.0, 0.0);
    } else {
        gl_FragColor = texture2D(state, p);
    }
}
