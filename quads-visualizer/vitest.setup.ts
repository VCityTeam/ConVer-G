if (typeof globalThis.WebGLRenderingContext === "undefined") {
  // @ts-expect-error - assigning a stub for a browser global in Node.
  globalThis.WebGLRenderingContext = class WebGLRenderingContext {};
}
if (typeof globalThis.WebGL2RenderingContext === "undefined") {
  // @ts-expect-error - assigning a stub for a browser global in Node.
  globalThis.WebGL2RenderingContext = class WebGL2RenderingContext {};
}
