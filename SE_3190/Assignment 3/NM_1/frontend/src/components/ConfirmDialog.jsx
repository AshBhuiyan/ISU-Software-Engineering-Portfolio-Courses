export default function ConfirmDialog({
  open,
  title,
  description,
  confirmLabel = "Confirm",
  cancelLabel = "Cancel",
  onConfirm,
  onCancel,
}) {
  if (!open) return null;
  return (
    <div className="fixed inset-0 z-40 flex items-center justify-center bg-black/70">
      <div className="bg-slate-900 rounded-xl shadow-xl p-6 w-full max-w-md border border-slate-700">
        <h2 className="text-lg text-white font-semibold mb-2">{title}</h2>
        <p className="text-sm text-slate-300 mb-4">{description}</p>
        <div className="flex justify-end gap-3">
          <button
            onClick={onCancel}
            className="px-3 py-1.5 text-sm rounded-full bg-slate-800 text-slate-200 hover:bg-slate-700"
          >
            {cancelLabel}
          </button>
          <button
            onClick={onConfirm}
            className="px-3 py-1.5 text-sm rounded-full bg-red-500 text-white hover:bg-red-400"
          >
            {confirmLabel}
          </button>
        </div>
      </div>
    </div>
  );
}
