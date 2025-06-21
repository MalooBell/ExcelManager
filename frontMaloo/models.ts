// ===== MODÈLES (MODELS) =====

export interface RowEntity {
  id?: number;
  dataJson: string;
  sheetIndex: number;
  fileName: string;
  createdAt?: Date;
}

export interface ModificationHistory {
  id?: number;
  rowEntityId: number;
  operationType: 'CREATE' | 'UPDATE' | 'DELETE';
  oldData?: string;
  newData?: string;
  timestamp: Date;
}


export interface AlertMessage {
  type: 'success' | 'error' | 'warning' | 'info';
  message: string;
  details?:string[];
}

// ===== DTOS =====

export interface UploadResponse {
  success: boolean;
  message: string;
  errors?: string[];
  processedRows: number;
}

export interface RowEntityDto {
  id?: number;
  sheetIndex: number;
  data: { [key: string]: any };
}

// ===== PAGE RESPONSE =====

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

// ===== PAGEABLE =====

export interface Pageable {
  page?: number;
  size?: number;
  sort?: string[];
}