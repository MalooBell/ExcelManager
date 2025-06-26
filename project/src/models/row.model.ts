export interface RowEntity {
  id: number;
  sheetIndex: number;
  data: { [key: string]: any };
  sheetName?: string; // Ajout pour identifier par sheetname
}

export interface ModificationHistory {
  id: number;
  rowEntityId: number;
  operationType: 'CREATE' | 'UPDATE' | 'DELETE';
  oldData: string | null;
  newData: string | null;
  timestamp: string;
  sheetName?: string; // Ajout pour identifier par sheetname
}