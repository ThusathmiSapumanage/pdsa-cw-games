import { useState } from 'react'
import GameLayoutShell from './GameLayoutShell'

function MinimumCostGame() {
  const [matrix, setMatrix] = useState([])
  const [logs, setLogs] = useState([])

  const handleStartGame = () => {
    fetch('/mincost/start')
      .then((res) => res.json())
      .then((data) => {
        setMatrix(data.matrix)
        setLogs((prevLogs) => [...prevLogs, 'Matrix generated'])
      })
      .catch(() => {
        setLogs((prevLogs) => [...prevLogs, 'Failed to fetch matrix from backend'])
      })
  }

  const handleNextStep = () => {
    setLogs((prevLogs) => [
      ...prevLogs,
      `Greedy Step ${prevLogs.length + 1}: choose smallest value`,
    ])
  }

  return (
    <GameLayoutShell
      matrix={matrix}
      logs={logs}
      onStartGame={handleStartGame}
      onNextStep={handleNextStep}
    />
  )
}

export default MinimumCostGame
