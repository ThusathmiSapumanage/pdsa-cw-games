function MatrixGrid({ matrix = [], assignments = [], blockedRows = [], blockedCols = [] }) {
  const hasRows = Array.isArray(matrix) && matrix.length > 0
  const columnCount = hasRows ? matrix[0].length : 0
  const isAssigned = (row, col) => assignments.some((item) => item.row === row && item.col === col)

  return (
    <div className="overflow-x-auto">
      <table className="w-full border-collapse border">
        <thead>
          <tr>
            <th className="border text-center p-2 bg-gray-50"></th>
            {Array.from({ length: columnCount }, (_, columnIndex) => (
              <th
                key={`employee-${columnIndex}`}
                className={`border text-center p-2 bg-gray-50 ${blockedCols.includes(columnIndex) ? 'opacity-40' : ''}`}
              >
                E{columnIndex + 1}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {hasRows ? (
            matrix.map((row, rowIndex) => (
              <tr key={`task-row-${rowIndex}`}>
                <th
                  className={`border text-center p-2 bg-gray-50 ${blockedRows.includes(rowIndex) ? 'opacity-40' : ''}`}
                >
                  T{rowIndex + 1}
                </th>
                {row.map((value, columnIndex) => (
                  (() => {
                    const cellIsAssigned = isAssigned(rowIndex, columnIndex)
                    const rowBlocked = blockedRows.includes(rowIndex)
                    const colBlocked = blockedCols.includes(columnIndex)
                    const dimmedClass = rowBlocked || colBlocked ? 'opacity-40' : ''
                    const assignedClass = cellIsAssigned ? 'bg-green-300 font-semibold opacity-100' : ''

                    return (
                      <td
                        key={`cell-${rowIndex}-${columnIndex}`}
                        className={`border text-center p-2 hover:bg-gray-100 ${dimmedClass} ${assignedClass}`}
                      >
                        {value}
                      </td>
                    )
                  })()
                ))}
              </tr>
            ))
          ) : (
            <tr>
              <td className="border text-center p-2 text-gray-500" colSpan={1}>
                No matrix data yet
              </td>
            </tr>
          )}
        </tbody>
      </table>
    </div>
  )
}

export default MatrixGrid