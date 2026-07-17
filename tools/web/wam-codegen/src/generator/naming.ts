/** Capitalizes the first character of a string. */
export function capitalize(s: string): string {
    return s.length === 0 ? s : s[0].toUpperCase() + s.slice(1);
}

/** Converts a dotted Java package name to a directory path. */
export function packageToPath(pkg: string): string {
    return pkg.replace(/\./g, "/");
}
