import { useState } from 'react'
import GameLayoutShell from './GameLayoutShell'

function MinimumCostGame() {
  const [matrix, setMatrix] = useState([])
  const [assignments, setAssignments] = useState([])
  const [blockedRows, setBlockedRows] = useState([])
  const [blockedCols, setBlockedCols] = useState([])
  const [logs, setLogs] = useState([])
  const [totalCost, setTotalCost] = useState(0)

  const handleStartGame = () => {
    fetch('/mincost/start')
      .then((res) => res.json())
      .then((data) => {
        const nextMatrix = Array.isArray(data.matrix) ? data.matrix : []
        setMatrix(nextMatrix)
        setAssignments([])
        setBlockedRows([])
        setBlockedCols([])
        setTotalCost(0)
        setLogs(['Matrix generated'])
      })
      .catch(() => {
        setLogs((prevLogs) => [...prevLogs, 'Failed to fetch matrix from backend'])
      })
  }

  const handleNextStep = () => {
    if (!matrix.length) {
      setLogs((prevLogs) => [...prevLogs, 'Load a matrix first by clicking Start Game'])
      return
    }

    if (assignments.length >= matrix.length) {
      setLogs((prevLogs) => [...prevLogs, 'All tasks assigned. Greedy algorithm complete.'])
      return
    }

    let minValue = Infinity
    let selectedRow = -1
    let selectedCol = -1

    for (let rowIndex = 0; rowIndex < matrix.length; rowIndex += 1) {
      if (blockedRows.includes(rowIndex)) {
        continue
      }

      for (let colIndex = 0; colIndex < matrix[rowIndex].length; colIndex += 1) {
        if (blockedCols.includes(colIndex)) {
          continue
        }

        if (matrix[rowIndex][colIndex] < minValue) {
          minValue = matrix[rowIndex][colIndex]
          selectedRow = rowIndex
          selectedCol = colIndex
        }
      }
    }

    if (selectedRow === -1 || selectedCol === -1) {
      setLogs((prevLogs) => [...prevLogs, 'No valid assignment found.'])
      return
    }

    const assignment = {
      row: selectedRow,
      col: selectedCol,
      cost: minValue,
    }

    const stepNumber = assignments.length + 1
    const message = `Step ${stepNumber}: Assign Task T${selectedRow + 1} -> Employee E${selectedCol + 1} (Cost ${minValue})`

    setAssignments((prevAssignments) => [...prevAssignments, assignment])
    setBlockedRows((prevRows) => [...prevRows, selectedRow])
    setBlockedCols((prevCols) => [...prevCols, selectedCol])
    setTotalCost((prevTotal) => prevTotal + minValue)
    setLogs((prevLogs) => [...prevLogs, message])
  }

  return (
    <GameLayoutShell
      matrix={matrix}
      assignments={assignments}
      blockedRows={blockedRows}
      blockedCols={blockedCols}
      logs={logs}
      totalCost={totalCost}
      onStartGame={handleStartGame}
      onNextStep={handleNextStep}
    />
  )
}

export default MinimumCostGame
