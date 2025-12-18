export default function LoadingSpinner({ label = "Loading..." }) {
  return (
    <div className="flex items-center justify-center py-12">
      <div className="flex flex-col items-center gap-3">
        <div className="h-10 w-10 border-4 border-skyvalorAccent border-t-transparent rounded-full animate-spin" />
        <span className="text-slate-300 text-sm">{label}</span>
      </div>
    </div>
  );
}
