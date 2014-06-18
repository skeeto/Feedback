#ifdef GL_ES
precision mediump float;
#endif

attribute vec2 quad;

varying vec3 coord;

void main() {
    coord = vec3(quad, 1.0);
    gl_Position = vec4(quad, 0, 1.0);
}
