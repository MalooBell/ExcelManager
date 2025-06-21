export interface FileEntity {
  id: number;
  fileName: string;
  uploadTimestamp: string;
  headersJson: string;
  totalRows: number;
}

export interface UploadResponse {
  success: boolean;
  message: string;
  errors: string[] | null;
  processedRows: number;
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