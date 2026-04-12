/**
 * Label Form Component
 *
 * Dimension labels for review items (accuracy, fluency, etc.)
 */

import styles from "./LabelForm.module.css";

export type LabelValue = "good" | "acceptable" | "needs-work" | "poor";

export type LabelValues = Record<string, LabelValue>;

interface LabelFormProps {
  dimensions: string[];
  values: LabelValues;
  onChange: (values: LabelValues) => void;
}

const LABEL_OPTIONS: Array<{ value: LabelValue; label: string; color: string }> = [
  { value: "good", label: "Good", color: "var(--token-colors-accent-green)" },
  { value: "acceptable", label: "Acceptable", color: "var(--token-colors-accent-cyan)" },
  { value: "needs-work", label: "Needs Work", color: "var(--token-colors-accent-amber)" },
  { value: "poor", label: "Poor", color: "var(--token-colors-accent-red)" },
];

export function LabelForm({ dimensions, values, onChange }: LabelFormProps) {
  const handleChange = (dimension: string, value: LabelValue) => {
    onChange({ ...values, [dimension]: value });
  };

  const formatDimension = (dimension: string) => {
    return dimension
      .replace(/_/g, " ")
      .replace(/\b\w/g, (c) => c.toUpperCase());
  };

  return (
    <div className={styles.form}>
      {dimensions.map((dimension) => (
        <div key={dimension} className={styles.dimension}>
          <label className={styles.dimensionLabel}>
            {formatDimension(dimension)}
          </label>
          <div className={styles.options}>
            {LABEL_OPTIONS.map((option) => (
              <button
                key={option.value}
                type="button"
                className={`${styles.option} ${
                  values[dimension] === option.value ? styles.optionSelected : ""
                }`}
                style={{
                  borderColor:
                    values[dimension] === option.value ? option.color : undefined,
                  color:
                    values[dimension] === option.value ? option.color : undefined,
                }}
                onClick={() => handleChange(dimension, option.value)}
              >
                {option.label}
              </button>
            ))}
          </div>
        </div>
      ))}
    </div>
  );
}
