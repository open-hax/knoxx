---
original_name: "2026.05.08.15.37.26.md"
title: "Explicit Tool IDs Breakage Analysis"
summary: "Evidence note on how explicit tool IDs alter contract tool policy semantics."
category: "contracts"
created: "2026-05-08"
---


 Evidence

 (己, p=0.96) Relevant code path:

 ```clojure
   (defn- explicit-tool-ids
     [contract]
     (->> (or (get-in contract [:data :tools]) [])
          ...))

   (defn- tool-policies-from-contract
     [config role contract]
     (let [explicit (explicit-tool-ids contract)
           role-tool-ids ...
           tool-ids (if (seq explicit) explicit role-tool-ids)]
       ...))
 ```

 (己, p=0.95) Consequence:

 - No :data/tools → use role/capability tools.
 - Has :data/tools → use only explicitly listed tools.

 (己, p=0.91) That means adding :data/tools is not harmless. It changes contract semantics.

 (己, p=0.9) The broader breakage likely came from recent runtime/contract changes, not from the original
 discord_visualist contract. Evidence already visible in logs:

 ```text
   [contracts] validation failed: ... template_synthesis_demo.edn — prompts.task should be a string
 ```

 That suggests at least one recent change introduced prompt forms/templates while the validator or loader path still
 expects prompt strings in some places.
