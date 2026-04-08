import MatrixGrid from './MatrixGrid'
import AlgorithmLog from './AlgorithmLog'

function GameLayoutShell({ matrix, logs, onStartGame, onNextStep }) {
  return (
    <div className="max-w-6xl mx-auto p-6 space-y-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <h1 className="text-3xl font-bold text-gray-900">Minimum Cost Assignment Game</h1>
        <div className="flex gap-3">
          <button
            type="button"
            onClick={onStartGame}
            className="px-4 py-2 rounded bg-blue-600 text-white hover:bg-blue-700"
          >
            Start Game
          </button>
          <button
            type="button"
            onClick={onNextStep}
            className="px-4 py-2 rounded bg-emerald-600 text-white hover:bg-emerald-700"
          >
            Next Step
          </button>
        </div>
      </div>

      <div className="grid grid-cols-2 gap-6">
        <div className="space-y-2">
          <h2 className="text-lg font-semibold text-gray-800">Greedy Algorithm Matrix</h2>
          <MatrixGrid matrix={matrix} />
        </div>

        <div className="space-y-2">
          <h2 className="text-lg font-semibold text-gray-800">Hungarian Algorithm Matrix</h2>
          <MatrixGrid matrix={matrix} />
        </div>
      </div>

      <div className="space-y-2">
        <h2 className="text-lg font-semibold text-gray-800">Algorithm Log</h2>
        <AlgorithmLog logs={logs} />
      </div>
    </div>
  )
}

export default GameLayoutShell
