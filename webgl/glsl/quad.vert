#ifdef GL_ES
precision mediump float;
#endif

attribute vec2 quad;
uniform mat3 placement;

varying vec3 coord;

void main() {
    vec2 position = (placement * vec3(quad, 1.0)).xy;
    coord = vec3(quad, 1.0);
    gl_Position = vec4(position, 0, 1.0);
}
