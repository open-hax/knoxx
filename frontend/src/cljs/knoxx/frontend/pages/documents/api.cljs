(ns knoxx.frontend.pages.documents.api
  "Document and lake operations through the session-aware browser HTTP boundary."
  (:require [knoxx.frontend.lib.documents :as documents]))

(def fetch-documents
  "List documents in the session's active lake."
  documents/fetch-documents)

(def upload-documents
  "Multipart upload of JS File objects, optionally auto-ingesting."
  documents/upload-documents)

(def delete-document
  "Delete the document named by its encoded relative path."
  documents/delete-document)

(def ingest-documents
  "Start ingestion using the supplied selection or full-run options."
  documents/ingest-documents)

(def restart-ingestion
  "Restart or resume ingestion with an explicit fresh-run flag."
  documents/restart-ingestion)

(def ingestion-progress
  "Read active ingestion and resumable checkpoint state."
  documents/ingestion-progress)

(def ingestion-history
  "Read completed ingestion runs for the active lake."
  documents/ingestion-history)

(def list-databases
  "List lake profiles and the active runtime profile."
  documents/list-databases)

(def create-database
  "Create a lake profile from its wire payload."
  documents/create-database)

(def activate-database
  "Activate the selected lake profile."
  documents/activate-database)

(def update-database
  "Patch the selected lake profile."
  documents/update-database)

(def delete-database
  "Delete the selected lake profile."
  documents/delete-database)

(def make-database-private
  "Restrict the selected lake profile to this browser session."
  documents/make-database-private)
