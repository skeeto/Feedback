#ifdef GL_ES
precision mediump float;
#endif

uniform vec4 color;

varying vec3 coord;

void main() {
    if (distance(coord.xy, vec2(0.0, 0.0)) < 1.0) {
        gl_FragColor = color;
    } else {
        gl_FragColor = vec4(0.0, 0.0, 0.0, 0.0);
    }
}
