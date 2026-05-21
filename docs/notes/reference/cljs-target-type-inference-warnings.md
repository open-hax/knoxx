---
original_name: "2026.05.19.11.18.05.md"
title: "CLJS Target Type Inference Warnings"
summary: "Reference note explaining ClojureScript target type inference warnings and why they accumulate."
category: "reference"
created: "2026-05-19"
---


## What the warnings mean

`Cannot infer target type` means the compiler cannot tell what JS object type is on the left side of a property access or method call, so it cannot safely reason about renaming/externs for advanced compilation. Thomas Heller notes that some of these warnings are effectively harmless in practice, especially around CLJS or Closure-native objects, but they still appear because the warning is about failed inference, not necessarily a proven runtime bug. [clojurescript](https://clojurescript.org/guides/externs)

## Why they accumulate

 `f`, `state`, and nested values like `state.sessions` are being used with direct JS property access such as `.-mtime` and `.-sessions` without enough type information attached, so every downstream access inherits ambiguity. Once you do things like `(js/Object.values (.-sessions state))`, the compiler loses even more structure unless `state` or the accessed value is hinted, so the same uncertain object fans out into dozens of warnings. [cljs.github](https://cljs.github.io/api/cljs.core/STARwarn-on-inferSTAR)

## Fixes that actually scale

The compiler docs recommend adding local type hints, for example `^js` or a more specific `^js/SomeType`, at the binding site so interop has a known target type. For interop-heavy code, the docs also explicitly say there is a point where repeated hinting becomes tedious and an externs file or a better-typed boundary/helper function is the cleaner move. [clojureverse](https://clojureverse.org/t/how-do-you-type-hint-in-cljs/5737)

treat this as a boundary-design problem, not a whack-a-warning game:
- Put JS interop behind small helpers with hinted arguments and return values, so only the edge talks `.-foo` and `js/Object.values` directly. [github](https://github.com/reagent-project/reagent/issues/585)
- Hint the *binding*, not every call site, because one good `^js` on `state` or `f` can collapse many warnings downstream. [clojurescript](https://clojurescript.org/guides/externs)
- Prefer `goog.object/get` or `aget` where appropriate for truly dynamic objects; Heller notes some native interop warnings can be avoided by using alternative access forms when no externs are actually needed. [code.thheller](https://code.thheller.com/blog/shadow-cljs/2017/11/06/improved-externs-inference.html)
- Keep `:infer-externs :auto` rather than globally broadening noise; Heller introduced `:auto` specifically so you don’t drown in warnings from everywhere, and shadow-cljs issue discussion notes behavior differences between modes. [github](https://github.com/thheller/shadow-cljs/issues/753)

## Policy, not patching

A sane policy is to keep infer warnings near zero in app code, isolate unavoidable ones behind tiny wrappers, and refuse warning regressions in CI so build output stays readable enough to preserve trust. [code.thheller](https://code.thheller.com/blog/shadow-cljs/2017/11/06/improved-externs-inference.html)


---

## Objectives

- Update the AGENTS.md file with the above policy advice
- 
