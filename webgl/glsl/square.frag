#ifdef GL_ES
precision mediump float;
#endif

uniform vec4 color;

varying vec3 coord;

const vec4 outside = vec4(0, 0, 0, 1);
const float delta = 0.1;

void main() {
    float dist = max(abs(coord.x), abs(coord.y));
    float a = smoothstep(1.0 - delta, 1.0, dist);
    gl_FragColor = mix(color, outside, a);
}
