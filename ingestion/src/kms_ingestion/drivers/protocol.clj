(ns kms-ingestion.drivers.protocol
  "Driver protocol for multi-source ingestion.")

(defprotocol Driver
  "Protocol for ingestion drivers.
   
   Each driver must implement:
   - discover: Find files in the source
   - extract: Read content from a file
   - extract-batch: Read content from multiple files (for efficiency)
   - get-state: Get driver-specific state for persistence
   - set-state: Restore driver state from persistence"
  
  (discover [this opts]
    "Discover files in the source.
     
     Opts:
     - :since - Only return files changed since this timestamp
     - :existing-hashes - Map of file-id -> content-hash for change detection
     
     Returns:
     {:total-files N
      :new-files N
      :changed-files N
      :deleted-files N
      :unchanged-files N
      :files [{:id \"...\" :path \"...\" :content-hash \"...\" :size N :modified-at #inst \"...\"}]}")
  
  (extract [this file-id]
    "Extract content for a single file.
     Returns {:id \"...\" :path \"...\" :content \"...\" :content-hash \"...\"}")
  
  (extract-batch [this file-ids]
    "Extract content for multiple files.
     Returns sequence of file maps with :content populated.")
  
  (get-state [this]
    "Get driver-specific state for persistence.
     Returns a JSON-serializable map.")
  
  (set-state [this state]
    "Restore driver state from persistence.")
  
  (close [this]
    "Clean up resources. Optional, default does nothing."))
