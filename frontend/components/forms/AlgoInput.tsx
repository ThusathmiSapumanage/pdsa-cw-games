"use client";

import React, { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';

interface AlgoInputProps {
    label: string;
    type: 'text' | 'email' | 'tel' | 'number';
    placeholder?: string;
    value: string;
    onChange: (value: string) => void;
    required?: boolean;
    nameType?: 'name' | 'phone' | 'email' | 'none';
}

export default function AlgoInput({ 
    label, 
    type, 
    placeholder, 
    value, 
    onChange, 
    required = false,
    nameType = 'none' 
}: AlgoInputProps) {
    const [error, setError] = useState<string | null>(null);
    const [isFocused, setIsFocused] = useState(false);

    const validate = (val: string) => {
        try {
            if (required && !val) {
                setError(`${label} is required`);
                return;
            }

            if (nameType === 'name' && val) {
                if (/\d/.test(val)) {
                    setError("Names cannot contain numbers");
                    return;
                }
            }

            if (nameType === 'phone' && val) {
                // Sri Lankan Phone Validation:
                // 1. Starts with 0 and has 10 digits (e.g., 0712345678)
                // 2. No 0 and has 9 digits (e.g., 712345678)
                // 3. Starts with +94 and followed by 9 digits (e.g., +94712345678)
                const slPhoneRegex = /^(?:0|(?:\+94))?[1-9]\d{8}$|^[1-9]\d{8}$/;
                if (!slPhoneRegex.test(val.replace(/\s/g, ''))) {
                    setError("Invalid Sri Lankan phone number format");
                    return;
                }
            }

            if (nameType === 'email' && val) {
                const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
                if (!emailRegex.test(val)) {
                    setError("Please enter a valid email address");
                    return;
                }
            }

            setError(null);
        } catch (err) {
            console.error("Validation Error:", err);
            setError("Unexpected validation error occurred");
        }
    };

    useEffect(() => {
        validate(value);
    }, [value]);

    const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        onChange(e.target.value);
    };

    return (
        <div className="w-full mb-8 group">
            <div className="flex justify-between items-end mb-2">
                <label className="text-[10px] font-mono uppercase tracking-[0.3em] text-white/40 group-focus-within:text-blue-500 transition-colors">
                    {label} {required && <span className="text-blue-600">*</span>}
                </label>
                <AnimatePresence>
                    {error && (
                        <motion.span 
                            initial={{ opacity: 0, y: 5 }}
                            animate={{ opacity: 1, y: 0 }}
                            exit={{ opacity: 0, y: 5 }}
                            className="text-[9px] font-bold text-red-500 uppercase tracking-widest"
                        >
                            {error}
                        </motion.span>
                    )}
                </AnimatePresence>
            </div>

            <div className={`relative transition-all duration-500 border-b ${error ? 'border-red-500' : isFocused ? 'border-blue-600' : 'border-white/10'}`}>
                <input
                    type={type}
                    value={value}
                    onChange={handleChange}
                    onFocus={() => setIsFocused(true)}
                    onBlur={() => setIsFocused(false)}
                    placeholder={placeholder}
                    className="w-full bg-transparent py-4 text-white placeholder:text-white/10 focus:outline-none text-sm md:text-base tracking-wide font-medium"
                />
                
                {/* Underline Animation */}
                <motion.div 
                    initial={false}
                    animate={{ width: isFocused ? '100%' : '0%' }}
                    className="absolute bottom-[-1px] left-0 h-[1px] bg-blue-500"
                />
            </div>
        </div>
    );
}
