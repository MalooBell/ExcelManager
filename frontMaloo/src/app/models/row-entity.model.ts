export interface RowEntity {
  id?: number;
  sheetIndex: number;
  data: { [key: string]: any };
}

export interface UploadResponse {
  success: boolean;
  message: string;
  errors?: string[];
  processedRows: number;
}

export interface ModificationHistory {
  id: number;
  rowEntityId: number;
  operationType: 'CREATE' | 'UPDATE' | 'DELETE';
  oldData?: string;
  newData?: string;
  timestamp: string;
}

export interface PageResponse<T> {
  content: T[];
  pageable: {
    pageNumber: number;
    pageSize: number;
    sort: {
      sorted: boolean;
      unsorted: boolean;
    };
  };
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
  numberOfElements: number;
}