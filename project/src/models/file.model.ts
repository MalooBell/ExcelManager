import { SheetEntity } from './sheet.model';

export interface FileEntity {
  id: number;
  fileName: string;
  uploadTimestamp: string;
  sheetCount: number;
  totalRows: number;
  processed: boolean;
  needsHeaderValidation: boolean; 
}

export interface UploadResponse {
  success: boolean;
  message: string;
  errors: string[] | null;
  processedRows: number;
  fileId: number | null;
  needsManualValidation: boolean;
}

export interface PageResponse<T> {
  content: T[];
  pageable: {
    pageNumber: number;
    pageSize: number;
    sort: {
      empty: boolean;
      sorted: boolean;
      unsorted: boolean;
    };
  };
  totalElements: number;
  totalPages: number;
  last: boolean;
  first: boolean;
  numberOfElements: number;
  size: number;
  number: number;
  sort: {
    empty: boolean;
    sorted: boolean;
    unsorted: boolean;
  };
  empty: boolean;
}