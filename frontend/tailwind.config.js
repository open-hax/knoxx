var config = {
    darkMode: 'class',
    content: ["./index.html", "./src/**/*.{ts,tsx}"],
    theme: {
        extend: {
            colors: {
                surface: "#f8fafc",
                card: "#ffffff",
                accent: "#0f766e",
                ink: "#111827"
            },
            boxShadow: {
                panel: "0 1px 2px rgba(0,0,0,0.08), 0 10px 30px rgba(2,6,23,0.06)"
            }
        }
    },
    plugins: []
};
export default config;
