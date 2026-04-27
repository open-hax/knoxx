---
title: "Agents Observability Surface Update"
category: frontend
created: 2026-04-24
original: 2026.04.24.13.24.00.md
status: note
---

# Knoxx frontend agent observability update
- change `Event Agents` view to just `Agents`
- move the `/ops/agents` view to a sub tab of `Agents` as `Agent Audit Logs`
  - the `Active`, `History`, and `Pause Refresh` buttons are moved from the top of the `Agent Audit Logs` view into the `Sessions` list on the left
  - Both the panels in this view are generalized to be ran on other pages
  - they can have page specific built in filters
  - they have search (both semantic and key word)
  - Guarantee every event in the session view is an openplanner event
  - The sessions view are an accurate representation of the agent's actual state (it is clear what the agent is seeing over the course of the session)
  - Multimodal events are well represented.
- remove `Event Agents` heading and sub text from the `Agents view`
- remove the system prompt from under the selected agent name in the `Agents` view, it's redundant
- move `Live runtime`, `Quick Reference` and `RUNTIME SNAPSHOT` sections to a 3 column view at the top
- For the selected agent, there is a filtered list of running jobs using the same component as the `Agent Audit Logs` left jobs list it has a 
