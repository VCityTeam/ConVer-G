import { type CSSProperties, type FC, type FormEvent, useEffect, useRef, useState } from "react";

export type NewNodeModalProps = {
  isOpen: boolean;
  errorMessage?: string | null;
  onSubmit: (name: string) => void;
  onCancel: () => void;
};

export const NewNodeModal: FC<NewNodeModalProps> = ({
  isOpen,
  errorMessage,
  onSubmit,
  onCancel,
}) => {
  const [name, setName] = useState("");
  const inputRef = useRef<HTMLInputElement | null>(null);

  useEffect(() => {
    if (!isOpen) {
      return;
    }

    setName("");

    const focusTimeout = window.setTimeout(() => {
      inputRef.current?.focus();
    }, 0);

    return () => window.clearTimeout(focusTimeout);
  }, [isOpen]);

  useEffect(() => {
    if (!isOpen) {
      return;
    }

    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        onCancel();
      }
    };

    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [isOpen, onCancel]);

  if (!isOpen) {
    return null;
  }

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const trimmedName = name.trim();
    if (!trimmedName) {
      return;
    }
    onSubmit(trimmedName);
  };

  return (
    <div style={overlayStyle}>
      <form onSubmit={handleSubmit} style={modalStyle}>
        <h3 style={{ margin: "0 0 8px 0" }}>Add a new node</h3>
        <p style={{ margin: "0 0 12px 0", color: "#555", fontSize: "0.9rem" }}>
          You double-clicked a spot on the canvas. Give the new node a memorable name
          and click Create.
        </p>

        <label htmlFor="new-node-name" style={{ fontSize: "0.85rem", marginBottom: "4px" }}>
          Node name
        </label>
        <input
          id="new-node-name"
          ref={inputRef}
          type="text"
          value={name}
          placeholder="e.g. CustomerOrderGraph"
          onChange={(event) => setName(event.target.value)}
          style={inputStyle}
        />

        {errorMessage && (
          <div style={errorStyle}>
            {errorMessage}
          </div>
        )}

        <div style={buttonRowStyle}>
          <button type="button" onClick={onCancel} style={secondaryButtonStyle}>
            Cancel
          </button>
          <button type="submit" style={primaryButtonStyle} disabled={!name.trim()}>
            Create node
          </button>
        </div>
      </form>
    </div>
  );
};

const overlayStyle: CSSProperties = {
  position: "fixed",
  inset: 0,
  backgroundColor: "rgba(0, 0, 0, 0.35)",
  display: "flex",
  alignItems: "center",
  justifyContent: "center",
  zIndex: 1000,
  padding: "16px",
};

const modalStyle: CSSProperties = {
  width: "min(360px, 100%)",
  backgroundColor: "white",
  borderRadius: "10px",
  padding: "20px",
  boxShadow: "0 12px 30px rgba(0,0,0,0.25)",
  display: "flex",
  flexDirection: "column",
  gap: "12px",
};

const inputStyle: CSSProperties = {
  padding: "10px",
  fontSize: "1rem",
  border: "1px solid #cbd5e1",
  borderRadius: "6px",
  outline: "none",
};

const buttonRowStyle: CSSProperties = {
  display: "flex",
  gap: "10px",
  justifyContent: "flex-end",
};

const baseButtonStyle: CSSProperties = {
  flex: 1,
  padding: "10px",
  borderRadius: "6px",
  fontSize: "0.95rem",
  cursor: "pointer",
  border: "none",
};

const primaryButtonStyle: CSSProperties = {
  ...baseButtonStyle,
  background: "linear-gradient(135deg, #2563eb, #0ea5e9)",
  color: "white",
  fontWeight: 600,
  transition: "opacity 0.2s ease",
};

const secondaryButtonStyle: CSSProperties = {
  ...baseButtonStyle,
  backgroundColor: "#e2e8f0",
  color: "#1e293b",
};

const errorStyle: CSSProperties = {
  padding: "8px",
  backgroundColor: "#fee2e2",
  color: "#b91c1c",
  borderRadius: "6px",
  fontSize: "0.85rem",
};
