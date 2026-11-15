import { ArrowDown, ArrowUp, ArrowUpDown } from 'lucide-react';

export default function SortableTh({
  tableId,
  columnKey,
  label,
  sorts,
  onSort,
  style,
  className,
  headerContent,
  ariaLabel,
}) {
  const index = sorts.findIndex((sort) => sort.key === columnKey);
  const activeSort = index >= 0 ? sorts[index] : null;
  const spokenLabel = ariaLabel || (typeof label === 'string' ? label : 'Column');

  return (
    <th
      className={className}
      style={style}
      aria-sort={!activeSort ? 'none' : activeSort.direction === 'asc' ? 'ascending' : 'descending'}
    >
      <button
        type="button"
        className="sortable-th-button"
        style={{ justifyContent: style?.textAlign === 'center' ? 'center' : undefined }}
        aria-label={`${spokenLabel}, ${activeSort ? `${activeSort.direction} sort priority ${index + 1}` : 'not sorted'}`}
        onClick={() => onSort(tableId, columnKey)}
      >
        <span>{label}</span>
        <span className={`sortable-th-indicator${activeSort ? ' active' : ''}`} aria-hidden="true">
          {activeSort?.direction === 'asc' ? <ArrowUp size={13} /> : null}
          {activeSort?.direction === 'desc' ? <ArrowDown size={13} /> : null}
          {!activeSort && <ArrowUpDown size={13} />}
          {activeSort && <sup>{index + 1}</sup>}
        </span>
      </button>
      {headerContent}
    </th>
  );
}
