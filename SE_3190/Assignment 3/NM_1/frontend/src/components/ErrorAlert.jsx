export default function ErrorAlert({ message }) {
  if (!message) return null;
  return (
    <div className="mb-4 rounded-lg border border-red-500/60 bg-red-500/10 px-4 py-3 text-sm text-red-200">
      {message}
    </div>
  );
}
