import { useState } from 'react'
import './App.css'
import MinimumCostGame from './games/MinCost/MinimumCostGame'
import Traffic from './games/Traffic/Traffic'

const GAMES = {
  minCost: 'minCost',
  traffic: 'traffic',
}

function App() {
  const [activeGame, setActiveGame] = useState(GAMES.minCost)

  return (
    <div className="app-shell">
      <header className="app-shell__header">
        <div className="app-shell__intro">
          <span className="app-shell__eyebrow">PDSA Games</span>
          <h1 className="app-shell__title">Algorithm game playground</h1>
          <p className="app-shell__description">
            Switch between the current Minimum Cost game and the new Traffic
            Simulation game from the same app entry point.
          </p>
        </div>

        <div className="app-shell__switcher" role="tablist" aria-label="Game selector">
          <button
            type="button"
            className={`app-shell__tab ${
              activeGame === GAMES.minCost ? 'app-shell__tab--active' : ''
            }`}
            onClick={() => setActiveGame(GAMES.minCost)}
          >
            Minimum Cost
          </button>
          <button
            type="button"
            className={`app-shell__tab ${
              activeGame === GAMES.traffic ? 'app-shell__tab--active' : ''
            }`}
            onClick={() => setActiveGame(GAMES.traffic)}
          >
            Traffic
          </button>
        </div>
      </header>

      <main className="app-shell__content">
        {activeGame === GAMES.minCost ? <MinimumCostGame /> : <Traffic />}
      </main>
    </div>
  )
}

export default App
