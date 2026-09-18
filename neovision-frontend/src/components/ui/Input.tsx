import type { InputHTMLAttributes, ReactNode, TextareaHTMLAttributes } from 'react';

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  error?: string;
  leftIcon?: ReactNode;
  hint?: string;
}

export function Input({ label, error, leftIcon, hint, className = '', id, ...rest }: InputProps) {
  const inputId = id || `input-${Math.random().toString(36).slice(2)}`;
  return (
    <div className="w-full">
      {label && (
        <label htmlFor={inputId} className="block text-sm font-medium text-slate-700 mb-1.5">
          {label}
        </label>
      )}
      <div className="relative">
        {leftIcon && (
          <div className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400 pointer-events-none">
            {leftIcon}
          </div>
        )}
        <input
          id={inputId}
          className={`
            w-full px-3.5 py-2.5 bg-white border rounded-lg text-sm
            placeholder:text-slate-400
            focus:outline-none focus:ring-2 focus:ring-offset-0 transition
            ${leftIcon ? 'pl-10' : ''}
            ${error
              ? 'border-red-300 focus:ring-red-200 focus:border-red-500'
              : 'border-slate-300 focus:ring-blue-200 focus:border-blue-500'}
            ${className}
          `}
          {...rest}
        />
      </div>
      {error && <p className="text-xs text-red-600 mt-1">{error}</p>}
      {!error && hint && <p className="text-xs text-slate-500 mt-1">{hint}</p>}
    </div>
  );
}

interface TextareaProps extends TextareaHTMLAttributes<HTMLTextAreaElement> {
  label?: string;
  error?: string;
  hint?: string;
}

export function Textarea({ label, error, hint, className = '', id, ...rest }: TextareaProps) {
  const textareaId = id || `textarea-${Math.random().toString(36).slice(2)}`;
  return (
    <div className="w-full">
      {label && (
        <label htmlFor={textareaId} className="block text-sm font-medium text-slate-700 mb-1.5">
          {label}
        </label>
      )}
      <textarea
        id={textareaId}
        className={`
          w-full px-3.5 py-2.5 bg-white border rounded-lg text-sm resize-y
          placeholder:text-slate-400
          focus:outline-none focus:ring-2 focus:ring-offset-0 transition
          ${error
            ? 'border-red-300 focus:ring-red-200 focus:border-red-500'
            : 'border-slate-300 focus:ring-blue-200 focus:border-blue-500'}
          ${className}
        `}
        {...rest}
      />
      {error && <p className="text-xs text-red-600 mt-1">{error}</p>}
      {!error && hint && <p className="text-xs text-slate-500 mt-1">{hint}</p>}
    </div>
  );
}
