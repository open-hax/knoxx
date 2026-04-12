/**
 * Ops Event Table
 *
 * Time-ordered event log with filtering.
 */

import { useState, useMemo } from "react";
import type { OpsEvent, OpsEventFilter, OpsEventType, OpsEventStatus } from "./ops-types";
import { STATUS_ICONS, STATUS_COLORS, TYPE_LABELS } from "./ops-types";
import styles from "./EventTable.module.css";

interface EventTableProps {
  events: OpsEvent[];
  onSelectEvent?: (event: OpsEvent) => void;
  selectedEventId?: string;
}

const ALL_TYPES: OpsEventType[] = ["ingestion", "embedding", "sync", "policy", "MT"];
const ALL_STATUSES: OpsEventStatus[] = ["done", "warn", "error", "running"];

export function EventTable({ events, onSelectEvent, selectedEventId }: EventTableProps) {
  const [selectedTypes, setSelectedTypes] = useState<OpsEventType[]>([]);
  const [selectedStatuses, setSelectedStatuses] = useState<OpsEventStatus[]>([]);
  const [dateStart, setDateStart] = useState<string>("");
  const [dateEnd, setDateEnd] = useState<string>("");

  const filter: OpsEventFilter = useMemo(() => ({
    types: selectedTypes.length > 0 ? selectedTypes : undefined,
    status: selectedStatuses.length > 0 ? selectedStatuses : undefined,
    dateRange: dateStart || dateEnd ? {
      start: dateStart ? new Date(dateStart) : new Date(0),
      end: dateEnd ? new Date(dateEnd) : new Date(),
    } : undefined,
  }), [selectedTypes, selectedStatuses, dateStart, dateEnd]);

  const filteredEvents = useMemo(() => {
    return events.filter((event) => {
      if (filter.types && !filter.types.includes(event.type)) return false;
      if (filter.status && !filter.status.includes(event.status)) return false;
      if (filter.dateRange) {
        const eventTime = new Date(event.time);
        if (eventTime < filter.dateRange.start || eventTime > filter.dateRange.end) return false;
      }
      return true;
    });
  }, [events, filter]);

  const toggleType = (type: OpsEventType) => {
    setSelectedTypes((prev) =>
      prev.includes(type) ? prev.filter((t) => t !== type) : [...prev, type]
    );
  };

  const toggleStatus = (status: OpsEventStatus) => {
    setSelectedStatuses((prev) =>
      prev.includes(status) ? prev.filter((s) => s !== status) : [...prev, status]
    );
  };

  const clearFilters = () => {
    setSelectedTypes([]);
    setSelectedStatuses([]);
    setDateStart("");
    setDateEnd("");
  };

  const formatTime = (date: Date) => {
    const d = new Date(date);
    return d.toLocaleTimeString("en-US", {
      hour: "numeric",
      minute: "2-digit",
      hour12: true,
    });
  };

  const hasFilters = selectedTypes.length > 0 || selectedStatuses.length > 0 || dateStart || dateEnd;

  return (
    <div className={styles.container}>
      {/* Filters */}
      <div className={styles.filters}>
        <div className={styles.filterGroup}>
          <span className={styles.filterLabel}>Type:</span>
          <div className={styles.filterButtons}>
            {ALL_TYPES.map((type) => (
              <button
                key={type}
                className={`${styles.filterButton} ${selectedTypes.includes(type) ? styles.filterButtonActive : ""}`}
                onClick={() => toggleType(type)}
              >
                {TYPE_LABELS[type]}
              </button>
            ))}
          </div>
        </div>

        <div className={styles.filterGroup}>
          <span className={styles.filterLabel}>Status:</span>
          <div className={styles.filterButtons}>
            {ALL_STATUSES.map((status) => (
              <button
                key={status}
                className={`${styles.filterButton} ${selectedStatuses.includes(status) ? styles.filterButtonActive : ""}`}
                onClick={() => toggleStatus(status)}
                style={selectedStatuses.includes(status) ? { borderColor: STATUS_COLORS[status] } : {}}
              >
                {STATUS_ICONS[status]} {status}
              </button>
            ))}
          </div>
        </div>

        <div className={styles.filterGroup}>
          <span className={styles.filterLabel}>Date:</span>
          <div className={styles.dateInputs}>
            <input
              type="date"
              className={styles.dateInput}
              value={dateStart}
              onChange={(e) => setDateStart(e.target.value)}
              placeholder="Start"
            />
            <span className={styles.dateSeparator}>–</span>
            <input
              type="date"
              className={styles.dateInput}
              value={dateEnd}
              onChange={(e) => setDateEnd(e.target.value)}
              placeholder="End"
            />
          </div>
        </div>

        {hasFilters && (
          <button className={styles.clearButton} onClick={clearFilters}>
            Clear filters
          </button>
        )}
      </div>

      {/* Table */}
      <div className={styles.tableWrapper}>
        <table className={styles.table}>
          <thead>
            <tr>
              <th className={styles.th}>Time</th>
              <th className={styles.th}>Type</th>
              <th className={styles.th}>Status</th>
              <th className={styles.th}>Summary</th>
            </tr>
          </thead>
          <tbody>
            {filteredEvents.length === 0 ? (
              <tr>
                <td colSpan={4} className={styles.emptyCell}>
                  No events match the current filters.
                </td>
              </tr>
            ) : (
              filteredEvents.map((event) => (
                <tr
                  key={event.id}
                  className={`${styles.tr} ${selectedEventId === event.id ? styles.trSelected : ""}`}
                  onClick={() => onSelectEvent?.(event)}
                >
                  <td className={styles.tdTime}>{formatTime(event.time)}</td>
                  <td className={styles.tdType}>{TYPE_LABELS[event.type]}</td>
                  <td className={styles.tdStatus}>
                    <span
                      className={styles.statusIcon}
                      style={{ color: STATUS_COLORS[event.status] }}
                    >
                      {STATUS_ICONS[event.status]}
                    </span>
                  </td>
                  <td className={styles.tdSummary}>{event.summary}</td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {/* Footer */}
      <div className={styles.footer}>
        <span className={styles.count}>
          {filteredEvents.length} event{filteredEvents.length !== 1 ? "s" : ""}
          {hasFilters && ` (filtered from ${events.length})`}
        </span>
      </div>
    </div>
  );
}

export default EventTable;
