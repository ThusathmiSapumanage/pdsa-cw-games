import GameLayoutShell from "../../../components/layouts/GameLayoutShell";
import AuthGate from "../../../components/auth/AuthGate";

export default function KnightsTourPage() {
  return (
    <AuthGate>
      <GameLayoutShell
        gameId="04"
        title="Knight's Tour"
        description="Complete chessboard traversal logic. Ensures the knight visits every square exactly once without overlap."
        controls={
          <div className="flex flex-col gap-4">
            <div className="mb-4">
              <label className="block text-[10px] text-white/40 uppercase tracking-widest mb-2 font-mono">
                Starting Coordinates
              </label>
              <div className="flex gap-4">
                <input
                  type="number"
                  placeholder="X (0-7)"
                  min="0"
                  max="7"
                  className="w-full bg-[#050505] border border-white/10 rounded px-4 py-3 text-sm text-white focus:outline-none focus:border-blue-500"
                />
                <input
                  type="number"
                  placeholder="Y (0-7)"
                  min="0"
                  max="7"
                  className="w-full bg-[#050505] border border-white/10 rounded px-4 py-3 text-sm text-white focus:outline-none focus:border-blue-500"
                />
              </div>
            </div>
            <button className="bg-white text-black py-3 rounded font-bold uppercase tracking-widest hover:bg-blue-600 hover:text-white transition-colors text-xs">
              Backtrack Path
            </button>
          </div>
        }
        visualization={
          <div className="text-center text-white/50 border-2 border-dashed border-blue-500/50 bg-blue-500/5 p-12 rounded-xl w-full flex flex-col items-center justify-center h-[500px]">
            <span className="font-mono text-xl md:text-3xl uppercase tracking-widest text-white mb-6 font-black">
              Live Matrix Panel
            </span>
            <p className="text-sm md:text-base max-w-xl mb-8 leading-relaxed">
              <strong className="text-blue-400 block mb-2 text-lg">
                ATTENTION TEAM MEMBERS:
              </strong>
              Work on the UI from this point forward. Remove this placeholder
              box and integrate the live graphical visualization of the{" "}
              <span className="text-white font-bold">
                Knight's Tour (Backtracking)
              </span>{" "}
              algorithm executing in real-time here.
            </p>
            <div className="px-8 py-3 bg-black/50 rounded-full border border-white/20 text-sm font-mono flex items-center gap-3 shadow-xl">
              <div className="w-2 h-2 rounded-full bg-red-500 animate-pulse" />
              // TODO: Integrate Java/Python backend here
            </div>
          </div>
        }
        results={
          <div className="flex flex-col gap-2 font-mono text-[10px] text-white/50">
            <p className="text-white">
              Status:{" "}
              <span className="text-yellow-500">Awaiting Initial Position</span>
            </p>
            <p>Path Confirmed: False</p>
            <p>Total Moves: 0</p>
          </div>
        }
      />
    </AuthGate>
  );
}
