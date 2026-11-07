export type DataRecord = Record<string, unknown>;
export type PermissionSpec = string | string[];
export interface FieldSpec {
  name: string; label: string;
  type?: 'text'|'uuid'|'email'|'tel'|'password'|'textarea'|'number'|'date'|'datetime'|'select'|'checkbox'|'string-list'|'array'|'file';
  required?: boolean; options?: string[] | {value:string;label:string}[];
  min?:number; max?:number; maxLength?:number; pattern?:string;
  defaultValue?: unknown; source?:string; help?:string; fields?:FieldSpec[];
  lookup?: {path:string;valueKey?:string;labelKey?:string;permission?:PermissionSpec};
}
export interface ColumnSpec {
  key:string; label:string; format?:'date'|'datetime'|'money'|'status'|'boolean'|'list'; module?:string;
}
export interface ActionSpec {
  id:string; label:string; path:string; method?:'POST'|'PUT'|'DELETE'|'UPLOAD'|'DOWNLOAD';
  scope:'collection'|'record'; permission?:PermissionSpec; allPermissions?:string[];
  fields?:FieldSpec[]; idempotent?:boolean; danger?:boolean; confirm?:string;
  payload?:DataRecord; body?:(values:DataRecord,record:DataRecord)=>unknown;
  when?:{field:string;values:unknown[]}; help?:string; resultColumns?:ColumnSpec[];
}
export interface ViewSpec {
  id:string; label:string; path?:string; dataKey?:string; permission?:PermissionSpec;
  columns?:ColumnSpec[]; fields?:ColumnSpec[]; actions?:ActionSpec[]; download?:boolean;
}
export interface ModuleSpec {
  id:string; title:string; description:string; group:string;
  permission?:PermissionSpec; allPermissions?:string[];
  endpoint?:string; detailPath?:string; queryFields?:FieldSpec[];
  columns:ColumnSpec[]; detailFields?:ColumnSpec[]; actions?:ActionSpec[]; views?:ViewSpec[];
  single?:boolean; paginated?:boolean; help?:string;
}
