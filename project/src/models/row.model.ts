export interface RowEntity {
  id: number;
  sheetIndex: number;
  data: { [key: string]: any };
}

export interface ModificationHistory {
  id: number;
  rowEntityId: number;
  operationType: 'CREATE' | 'UPDATE' | 'DELETE';
  oldData: string | null;
  newData: string | null;
  timestamp: string;
}