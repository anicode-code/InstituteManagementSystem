import { useState } from 'react';

const EMPTY_SORTS = [];

function getValue(row, key, accessors) {
  const accessor = accessors?.[key];
  if (typeof accessor === 'function') return accessor(row);
  return key.split('.').reduce((value, part) => value?.[part], row);
}

function compareValues(left, right) {
  if (left == null || left === '') return right == null || right === '' ? 0 : 1;
  if (right == null || right === '') return -1;
  if (typeof left === 'number' && typeof right === 'number') return left - right;
  if (typeof left === 'boolean' && typeof right === 'boolean') return Number(left) - Number(right);
  if (left instanceof Date && right instanceof Date) return left.getTime() - right.getTime();
  return String(left).localeCompare(String(right), undefined, { numeric: true, sensitivity: 'base' });
}

export function sortRows(rows, sorts, accessors) {
  if (!sorts.length) return rows;
  return rows
    .map((row, originalIndex) => ({ row, originalIndex }))
    .sort((left, right) => {
      for (const { key, direction } of sorts) {
        const result = compareValues(
          getValue(left.row, key, accessors),
          getValue(right.row, key, accessors),
        );
        if (result !== 0) return direction === 'desc' ? -result : result;
      }
      return left.originalIndex - right.originalIndex;
    })
    .map(({ row }) => row);
}

export function cycleSorts(currentSorts, key) {
  const existing = currentSorts.find((sort) => sort.key === key);
  if (!existing) return [...currentSorts, { key, direction: 'asc' }];
  if (existing.direction === 'asc') {
    return currentSorts.map((sort) => (
      sort.key === key ? { ...sort, direction: 'desc' } : sort
    ));
  }
  return [];
}

export function useTableSorting() {
  const [sortsByTable, setSortsByTable] = useState({});

  const toggleSort = (tableId, key) => {
    setSortsByTable((current) => {
      return { ...current, [tableId]: cycleSorts(current[tableId] || [], key) };
    });
  };

  const getSorts = (tableId) => sortsByTable[tableId] || EMPTY_SORTS;
  const getSortedRows = (tableId, rows, accessors) => (
    sortRows(rows, getSorts(tableId), accessors)
  );

  return { getSorts, getSortedRows, toggleSort };
}
