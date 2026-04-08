function MatrixGrid({ matrix = [] }) {
  const hasRows = Array.isArray(matrix) && matrix.length > 0
  const columnCount = hasRows ? matrix[0].length : 0

  return (
    <div className="overflow-x-auto">
      <table className="w-full border-collapse border">
        <thead>
          <tr>
            <th className="border text-center p-2 bg-gray-50"></th>
            {Array.from({ length: columnCount }, (_, columnIndex) => (
              <th key={`employee-${columnIndex}`} className="border text-center p-2 bg-gray-50">
                E{columnIndex + 1}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {hasRows ? (
            matrix.map((row, rowIndex) => (
              <tr key={`task-row-${rowIndex}`}>
                <th className="border text-center p-2 bg-gray-50">T{rowIndex + 1}</th>
                {row.map((value, columnIndex) => (
                  <td
                    key={`cell-${rowIndex}-${columnIndex}`}
                    className="border text-center p-2 hover:bg-gray-100"
                  >
                    {value}
                  </td>
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
