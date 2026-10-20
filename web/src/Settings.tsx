import { useEffect, useState } from "react";
import { checkHealth, getApiBase, isTauri, setApiBase } from "./config";

interface Props {
  open: boolean;
  onClose: () => void;
  /** Called after the base changes so the app can refetch with the new host. */
  onApplied: () => void;
}

type ProbeState =
  | { kind: "idle" }
  | { kind: "checking" }
  | { kind: "ok"; version?: string }
  | { kind: "fail"; error: string };

/**
 * Backend settings dialog.
 *
 * Needed because the backend hosting decision is remote/self-hosted: the
 * client ships thin and the user supplies an API base URL. Empty input means
 * "same origin", which is what the Vite dev proxy and any reverse-proxied
 * deploy use.
 */
export default function Settings({ open, onClose, onApplied }: Props) {
  const [draft, setDraft] = useState("");
  const [probe, setProbe] = useState<ProbeState>({ kind: "idle" });
  const [saved, setSaved] = useState(false);

  useEffect(() => {
    if (open) {
      setDraft(getApiBase());
      setProbe({ kind: "idle" });
      setSaved(false);
    }
  }, [open]);

  if (!open) return null;

  const test = async () => {
    setProbe({ kind: "checking" });
    // Apply to a temp value so checkHealth() hits the new host, not the old.
    const previous = getApiBase();
    setApiBase(draft);
    const result = await checkHealth();
    if (result.ok) setProbe({ kind: "ok", version: result.version });
    else setProbe({ kind: "fail", error: result.error ?? "Unknown error" });
    // Leave the draft applied only if the probe succeeded; otherwise revert.
    if (!result.ok) setApiBase(previous);
  };

  const apply = () => {
    setApiBase(draft);
    setSaved(true);
    onApplied();
  };

  return (
    <div className="modal-backdrop" onClick={onClose} role="presentation">
      <div
        className="modal"
        onClick={(e) => e.stopPropagation()}
        role="dialog"
        aria-modal="true"
        aria-label="Backend settings"
      >
        <h2>Backend</h2>
        <p className="modal-hint">
          Point this client at your self-hosted MovieBox API. Leave blank to use the same origin
          (dev proxy or reverse-proxied deploy).
          {isTauri() && " Running in the desktop app."}
        </p>

        <label className="modal-label" htmlFor="api-base">
          API base URL
        </label>
        <input
          id="api-base"
          className="modal-input"
          value={draft}
          onChange={(e) => {
            setDraft(e.target.value);
            setSaved(false);
            setProbe({ kind: "idle" });
          }}
          placeholder="http://127.0.0.1:8000"
          spellCheck={false}
          autoComplete="off"
        />

        <div className="modal-probe">
          {probe.kind === "checking" && <span className="modal-note">Checking…</span>}
          {probe.kind === "ok" && (
            <span className="modal-ok">✓ Connected{probe.version ? ` · v${probe.version}` : ""}</span>
          )}
          {probe.kind === "fail" && <span className="modal-err">✗ {probe.error}</span>}
          {saved && <span className="modal-ok">✓ Saved</span>}
        </div>

        <div className="modal-actions">
          <button className="btn-ghost" onClick={onClose}>
            Cancel
          </button>
          <button className="btn-ghost" onClick={() => void test()} disabled={probe.kind === "checking"}>
            Test
          </button>
          <button className="btn-primary" onClick={apply}>
            Save
          </button>
        </div>
      </div>
    </div>
  );
}
