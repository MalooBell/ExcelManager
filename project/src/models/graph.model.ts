export interface GraphRequest {
  chartType: 'pie' | 'bar' | 'line';
  categoryColumn: string;
  valueColumns: string[];
  aggregationType?: 'COUNT' | 'SUM';
  groupingColumn?: string;
  limit?: number | null;
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