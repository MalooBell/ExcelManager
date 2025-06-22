export interface GraphRequest {
  chartType: 'pie' | 'bar' | 'line';
  categoryColumn: string;
  valueColumns: string[];
  aggregationType?: 'COUNT' | 'SUM'; // <-- AJOUTER CETTE LIGNE
  groupingColumn?: string; // <-- AJOUTER CETTE LIGNE
  limit?: number | null; // <-- AJOUTER
}

export interface GraphData {
  labels: string[];
  datasets: {
    data: number[];
    label?: string;
    backgroundColor?: string[];
    borderColor?: string[];
  }[];
}