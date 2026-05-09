goog.provide("helix.impl.classes");
helix.impl.classes.createComponent = function createComponent(superclass, spec, statics) {
  const helix$impl$classes$classdecl$var0 = class extends superclass {
    constructor(props) {
      super(props);
      if (spec.constructor) {
        spec.constructor.call(this, this);
      }
      for (let key in spec) {
        if (key !== "constructor") {
          let prop = spec[key];
          if (typeof prop === "function") {
            this[key] = prop.bind(this, this);
          } else {
            this[key] = prop;
          }
        }
      }
    }
  };
  let component = helix$impl$classes$classdecl$var0;
  for (let key in statics) {
    let prop = statics[key];
    if (typeof prop === "function") {
      component[key] = prop.bind(component, component);
    } else {
      component[key] = prop;
    }
  }
  return component;
};

//# sourceMappingURL=helix.impl.classes.js.map
