#ifdef GL_ES
precision mediump float;
#endif

uniform sampler2D state;
uniform mat3 transform;

varying vec3 coord;

void main() {
    vec2 transformed = (transform * coord).st;
    gl_FragColor = texture2D(state, transformed / 2.0 + 0.5);
}
